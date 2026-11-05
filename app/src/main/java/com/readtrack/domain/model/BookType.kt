package com.readtrack.domain.model

/**
 * 书籍类型默认值。
 *
 * 类型改为用户可自定义后，这里只保留常量，不再使用枚举：
 * - [DEFAULT] 作为新建书籍的默认类型，以及迁移/兜底时使用
 * - [DEFAULTS] 是数据库迁移时写入的内置类型，顺序即展示顺序
 */
object BookTypes {
    const val DEFAULT = "小说"

    val DEFAULTS = listOf("小说", "漫画", "有声书")
}
