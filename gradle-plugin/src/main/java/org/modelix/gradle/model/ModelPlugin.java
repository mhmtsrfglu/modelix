package org.modelix.gradle.model;

import kotlin.text.Charsets;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RelativePath;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.JavaExec;
import org.gradle.process.ExecResult;
import org.modelix.gradle.model.EnvironmentLoader.Key;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public class ModelPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project_) {
        ModelixModelSettings settings = project_.getExtensions().create("modelixModel", ModelixModelSettings.class);
        project_.afterEvaluate((Project project) -> {
            System.out.println("===========================");
            System.out.println("Modelix model plugin loaded");
            System.out.println("===========================");
            System.out.println("  settings loaded from modelixModel.");
            System.out.println("  mps path            : " + settings.getMpsPath());
            System.out.println("  mps extensions path : " + settings.getMpsExtensionsArtifactsPath());
            System.out.println("  modelix path        : " + settings.getModelixArtifactsPath());
            settings.validate();

            Manifest manifest = readManifest();
            String modelixVersion = manifest.getMainAttributes().getValue("modelix-Version");
            Configuration genConfig = project.getConfigurations().detachedConfiguration(
                project.getDependencies().create("org.modelix:gradle-plugin:" + modelixVersion)
            );

            final Configuration mpsConfig;
            final Configuration pluginsConfig;
            if (settings.usingExistingMps()) {
                mpsConfig = null;
                pluginsConfig = null;
                // We are using an existing MPS. We also expect the user to add the version of MPS Extensions and
                // Modelix that they intend to use
            } else {
                // We are not using an existing MPS, therefore we will add one and we will add dependencies
                // to MPS Extensions and Modelix as well
                String mpsVersion = manifest.getMainAttributes().getValue("MPS-Version");
                String extensionsVersion = manifest.getMainAttributes().getValue("MPS-extensions-Version");
                mpsConfig = project.getConfigurations().detachedConfiguration(
                        project.getDependencies().create("com.jetbrains:mps:" + mpsVersion )
                );
                pluginsConfig = project.getConfigurations().detachedConfiguration(
                        project.getDependencies().create("de.itemis.mps:extensions:" + extensionsVersion),
                        project.getDependencies().create("org.modelix:mps-model-plugin:" + modelixVersion));
            }

            final Copy copyMpsTask;
            final Copy copyMpsModelPluginTask;
            if (settings.usingExistingMps()) {
                copyMpsTask = null;
                copyMpsModelPluginTask = null;
                // When using an existing MPS we do not need to copy plugins
            } else {
                // When not using an existing MPS we do need to copy plugins under the downloaded MPS
                copyMpsTask = project.getTasks().create("copyMpsForModelixExport", Copy.class, copy -> {
                    copy.from(mpsConfig.resolve().stream().map(project::zipTree).collect(Collectors.toList()));
                    copy.into(new File("mpsForModelixExport"));
                });
                copyMpsModelPluginTask = project.getTasks().create("copyMpsModelPluginForModelixExport", Copy.class, copy -> {
                    copy.dependsOn(copyMpsTask);
                    copy.from(pluginsConfig.resolve().stream().map(it -> project.zipTree(it)).collect(Collectors.toList()));
                    copy.eachFile(fcd -> {
                        if (fcd.getRelativePath().getSegments()[0].equals("de.itemis.mps.extensions")) {
                            // remove first folder from output path
                            fcd.setRelativePath(new RelativePath(true, Arrays.stream(fcd.getRelativePath().getSegments()).skip(1).toArray(String[]::new)));
                        }
                    });
                    copy.into(new File(new File("mpsForModelixExport"), "plugins"));
                });
            }

            project.getTasks().create("downloadModel", JavaExec.class, javaExec -> {
                final boolean usingExistingMPS = settings.usingExistingMps();
                File mpsLocation = settings.getMpsLocation();
                File mpsExtensionsLocation = settings.getMpsExtensionsLocation();
                File modelixLocation = settings.getModelixLocation();
                System.out.println("=========================================");
                System.out.println("Modelix model plugin: Download model task");
                System.out.println("=========================================");
                System.out.println("  using existing MPS      : " + usingExistingMPS);
                System.out.println("  mps location            : " + mpsLocation);
                System.out.println("  mps extensions location : " + mpsExtensionsLocation);
                System.out.println("  modelix location        : " + modelixLocation);
                System.out.println("  server URL              : " + settings.getServerUrl());
                System.out.println("  repository ID           : " + settings.getRepositoryId());
                System.out.println("  branch name             : " + settings.getBranchName());
                System.out.println("  additional libraries    : " + settings.getAdditionalLibrariesAsString());
                System.out.println("  additional library dirs : " + settings.getAdditionalLibraryDirsAsString());
                System.out.println("  additional plugins      : " + settings.getAdditionalPluginsAsString());
                System.out.println("  additional plugin dirs  : " + settings.getAdditionalPluginDirsAsString());
                if (usingExistingMPS) {
                    // no dependencies
                } else {
                    javaExec.dependsOn(copyMpsTask, copyMpsModelPluginTask);
                }
                javaExec.setDescription("Export models from modelix model server to MPS files");
                javaExec.classpath(project.fileTree(new File(mpsLocation, "lib")).include("**/*.jar"));
                javaExec.classpath(genConfig);
                javaExec.args(
                        Key.SERVER_URL.getCode(), settings.getServerUrl(),
                        Key.REPOSITORY_ID.getCode(), settings.getRepositoryId(),
                        Key.BRANCH_NAME.getCode(), settings.getBranchName(),
                        Key.ADDITIONAL_LIBRARIES.getCode(), settings.getAdditionalLibrariesAsString(),
                        Key.ADDITIONAL_LIBRARY_DIRS.getCode(), settings.getAdditionalLibraryDirsAsString(),
                        Key.ADDITIONAL_PLUGINS.getCode(), settings.getAdditionalPluginsAsString(),
                        Key.ADDITIONAL_PLUGIN_DIRS.getCode(), settings.getAdditionalPluginDirsAsString()
                );
                if (settings.getMpsExtensionsArtifactsPath() != null) {
                    javaExec.args(Key.MPS_EXTENSIONS_PATH.getCode(), settings.getMpsExtensionsArtifactsPath());
                }
                if (settings.getModelixArtifactsPath() != null) {
                    javaExec.args(Key.MODELIX_PATH.getCode(), settings.getModelixArtifactsPath());
                }
                if (settings.isDebug()) javaExec.setDebug(true);
                javaExec.getTimeout().set(Duration.ofSeconds(settings.getTimeout()));
                javaExec.setIgnoreExitValue(true);
                javaExec.setMain(ExportMain.class.getName());
                javaExec.doLast(task -> {
                    System.out.println("After execution");
                    ExecResult execResult = javaExec.getExecutionResult().get();
                    int exitValue = execResult.getExitValue();
                    if (exitValue != 0) {
                        System.err.println("Execution of ExportMain failed. Exit code: " + exitValue);
                    }
                });
            });
        });
    }

    private Manifest readManifest() {
        Enumeration<URL> resources = null;
        try {
            resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                try {
                    Manifest manifest = new Manifest(resources.nextElement().openStream());
                    if (manifest.getMainAttributes().getValue("modelix-Version") != null) {
                        return manifest;
                    }
                } catch (IOException ex) {
                    throw new RuntimeException("Failed to read MANIFEST.MF", ex);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read MANIFEST.MF", ex);
        }
        throw new RuntimeException("No MANIFEST.MF found containing 'modelix-Version'");
    }
}
