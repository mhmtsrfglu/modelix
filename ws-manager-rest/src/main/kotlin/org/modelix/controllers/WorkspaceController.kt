package org.modelix.controllers

import org.modelix.managers.WorkspaceManager
import org.modelix.response.Response
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin

@RestController
@RequestMapping("/workspace/api")
@CrossOrigin
class WorkspaceController {

    @Autowired
    lateinit var manager: WorkspaceManager

    @GetMapping("/hello")
    fun test(): String = "Hello from Kotlin"

    @PostMapping("/new")
    fun newWorkspace(): Response {
        val workpsace = manager.newWorkspace()
        val message = mapOf("workspaceId" to workpsace.id)
        return Response(HttpStatus.OK, "Success", message)
    }

    @GetMapping("{workspaceId}/hash")
    fun getWorkspaceHash(@PathVariable workspaceId: String): Response {

        val workspaceAndHash = manager.getWorkspaceForId(workspaceId)

        return if (workspaceAndHash == null) {
            Response(HttpStatus.NOT_FOUND,"Workspace not found","")
        } else {
            Response(HttpStatus.OK,"Success", workspaceAndHash.second.toString())
        }
    }

}

