package org.modelix.controllers

import org.modelix.managers.WorkspaceManager
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/workspace/api")
class WorkspaceController {

    val manager = WorkspaceManager()

    @GetMapping("/hello")
    fun test(): String = "Hello from Kotlin"
}

