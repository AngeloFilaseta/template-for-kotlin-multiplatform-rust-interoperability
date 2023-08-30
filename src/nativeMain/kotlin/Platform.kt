import plus.plus

actual object Platform {
    const val x = 2
    const val y = 2
    actual val name = "Native. Also $x + $y is equal to ${plus(x, y)} in Rust"
}
