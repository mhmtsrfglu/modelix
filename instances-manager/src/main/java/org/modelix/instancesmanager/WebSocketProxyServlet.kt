/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modelix.instancesmanager

import org.modelix.instancesmanager.DeploymentManager
import java.lang.InterruptedException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import org.modelix.model.client.RestWebModelClient
import io.kubernetes.client.openapi.apis.AppsV1Api
import io.kubernetes.client.openapi.models.V1DeploymentList
import io.kubernetes.client.openapi.models.V1Deployment
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.ApiException
import javax.servlet.http.HttpServletRequest
import java.io.IOException
import java.lang.RuntimeException
import io.kubernetes.client.openapi.apis.CoreV1Api
import io.kubernetes.client.openapi.models.V1PodList
import io.kubernetes.client.openapi.models.V1Pod
import io.kubernetes.client.openapi.models.V1EnvVar
import io.kubernetes.client.openapi.models.V1ServiceList
import io.kubernetes.client.openapi.models.V1Service
import io.kubernetes.client.openapi.models.V1ServicePort
import io.kubernetes.client.openapi.models.V1Container
import io.kubernetes.client.openapi.models.V1ResourceRequirements
import org.apache.log4j.Logger
import java.util.LinkedList
import org.modelix.instancesmanager.DeploymentTimeouts
import java.lang.NumberFormatException
import javax.servlet.http.HttpServletResponse
import org.modelix.instancesmanager.ProxyServletWithWebsocketSupport
import java.net.URISyntaxException
import javax.servlet.ServletException
import org.eclipse.jetty.proxy.ProxyServlet
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketListener
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer
import javax.servlet.ServletContext

abstract class WebSocketProxyServlet : WebSocketServlet() {
    protected abstract fun redirect(request: ServletUpgradeRequest?): URI?
    override fun configure(factory: WebSocketServletFactory) {
        factory.policy.maxTextMessageSize = 20 * 1024 * 1024
        factory.creator = WebSocketCreator { req, resp ->
            val redirectURL = redirect(req) ?: return@WebSocketCreator null
            object : WebSocketListener {
                private val client = WebSocketClient()
                private var sessionA: Session? = null
                private var sessionB: Session? = null
                override fun onWebSocketConnect(session: Session) {
                    sessionA = session
                    try {
                        client.start()
                        client.policy.maxTextMessageSize = 20 * 1024 * 1024
                        val redirectURL = redirect(req)
                        client.connect(object : WebSocketListener {
                            override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {}
                            override fun onWebSocketText(message: String) {
                                try {
                                    sessionA!!.remote.sendString(message)
                                } catch (e: IOException) {
                                    throw RuntimeException(e)
                                }
                            }

                            override fun onWebSocketClose(statusCode: Int, reason: String) {
                                sessionA!!.close(statusCode, reason)
                            }

                            override fun onWebSocketConnect(session: Session) {
                                sessionB = session
                            }

                            override fun onWebSocketError(cause: Throwable) {
                                LOG.error("", cause)
                            }
                        }, redirectURL, ClientUpgradeRequest()).get()
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }

                override fun onWebSocketText(message: String) {
                    try {
                        sessionB!!.remote.sendString(message)
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }

                override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
                    try {
                        sessionB!!.remote.sendBytes(ByteBuffer.wrap(payload, offset, len))
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }
                }

                override fun onWebSocketClose(statusCode: Int, reason: String) {
                    sessionB!!.close(statusCode, reason)
                    try {
                        client.stop()
                    } catch (e: Exception) {
                        LOG.error("", e)
                    }
                }

                override fun onWebSocketError(cause: Throwable) {
                    LOG.error("", cause)
                }
            }
        }
    }

    companion object {
        private val LOG = Logger.getLogger(WebSocketProxyServlet::class.java)
    }
}