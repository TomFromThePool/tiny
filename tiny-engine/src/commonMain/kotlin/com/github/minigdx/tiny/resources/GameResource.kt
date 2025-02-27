package com.github.minigdx.tiny.resources

interface GameResource {
    /**
     * Index of this game resource, by type.
     */
    val index: Int

    /**
     * Name of the resource.
     */
    val name: String

    /**
     * Type of the resource.
     */
    val type: ResourceType

    /**
     * The resource needs to be reloaded ?
     */
    var reload: Boolean
}
