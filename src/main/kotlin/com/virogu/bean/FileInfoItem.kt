package com.virogu.bean

//drwxr-xr-x  -rwxrwxr-x
//-：普通文件
//d：文件夹
//l：符号连接(软连接/快捷方式) 后面会用 -> 打印出指向的真实文件
enum class FileType(val sortIndex: Int) {
    DIR(0),
    LINK(0),
    FILE(1),
    OTHER(2)
}

sealed class FileItem(open val name: String = "")

sealed class FileTipsItem(open val msg: String) : FileItem(msg) {
    data class Info(override val msg: String) : FileTipsItem(msg)
    data class Error(override val msg: String) : FileTipsItem(msg)
}

data class FileInfoItem(
    override val name: String = "",
    val parentPath: String = "",
    val path: String = "",
    val type: FileType = FileType.OTHER,
    val size: String = "",
    val modificationTime: String = "",
    val permissions: String = "",
) : FileItem(name) {

    val isDirectory = type == FileType.DIR

    companion object {
        val ROOT = FileInfoItem(
            name = "",
            path = "",
            type = FileType.DIR,
        )
    }
}
