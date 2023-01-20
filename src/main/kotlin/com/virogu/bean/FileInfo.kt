package com.virogu.bean

//drwxr-xr-x  -rwxrwxr-x
//-：普通文件
//d：文件夹
//l：符号连接(软连接/快捷方式) 后面会用 -> 打印出指向的真实文件
enum class FileType(val sortIndex: Int) {
    DIR(0),
    LINK(0),
    FILE(1),
    OTHER(2),
    ERROR(3),
    TIPS(4)
}

data class FileInfo(
    val name: String = "",
    val parentPath: String = "",
    val path: String = "",
    val type: FileType = FileType.OTHER,
    val size: String = "",
    val modificationTime: String = "",
    val permissions: String = "",
) {
    companion object {
        val ROOT = FileInfo(
            name = "",
            path = "",
            type = FileType.DIR,
        )
    }
}
