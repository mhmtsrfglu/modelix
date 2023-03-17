package org.modelix.controllers

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.encodeToString
import org.modelix.managers.WorkspaceManager
import org.modelix.response.Response
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin

@RestController
@RequestMapping("/workspace/api")
@CrossOrigin
class WorkspaceController {

//    @Autowired
//    lateinit var manager: WorkspaceManager

    @GetMapping("/test")
    fun test (): ResponseEntity<Response> {
        val str:String = "mehmet, ahmet, hüseyin,ali,muzaffer"

        return ResponseEntity(Response("Success",str.split(",").toSet()),HttpStatus.OK)
    }

//    @GetMapping("/getWorkspaces")
//    fun getWorkspaces (): ResponseEntity<Response> {
//        val workspaces = manager.getWorkspaceIds()
//            .mapNotNull { manager.getWorkspaceForId(it) }
//
//        return ResponseEntity(Response("Success",workspaces),HttpStatus.OK)
//    }
//
//    @PostMapping("/new")
//    fun newWorkspace(): ResponseEntity<Response> {
//        val workpsace = manager.newWorkspace()
//        val message = mapOf("workspaceId" to workpsace.id)
//        return ResponseEntity(Response("Success", message),HttpStatus.OK)
//    }
//
//    @GetMapping("{workspaceId}/hash")
//    fun getWorkspaceHash(@PathVariable workspaceId: String): ResponseEntity<Response> {
//
//        val workspaceAndHash = manager.getWorkspaceForId(workspaceId)
//
//        return if (workspaceAndHash == null) {
//            return ResponseEntity<Response>(Response("Workspace not found"),HttpStatus.NOT_FOUND)
//        } else {
//            return ResponseEntity(Response("Success", workspaceAndHash.second.toString()),HttpStatus.OK)
//        }
//    }
//
//    @GetMapping("{workspaceId}/edit")
//    fun editWorkspace(@PathVariable workspaceId: String): ResponseEntity<Response>{
//
//        if (workspaceId == null) {
//            return ResponseEntity(Response("Workspace ID is missing"),HttpStatus.BAD_REQUEST)
//        }
//        val workspaceAndHash = manager.getWorkspaceForId(workspaceId)
//        if (workspaceAndHash == null) {
//            return ResponseEntity(Response("Workspace $workspaceId not found"),HttpStatus.NOT_FOUND)
//        }
//        val (workspace, workspaceHash) = workspaceAndHash
//        val yaml = Yaml.default.encodeToString(workspace)
//
//        return ResponseEntity(Response("test"),HttpStatus.OK)
//
//    }

}

