package entao

import io.github.yangentao.types.tid
import io.github.yangentao.types.vtasks
import kotlin.test.Test

class VTaskTest {
    @Test
    fun a() {
        for (i in 0..100) {
            vtasks.submit {
                Thread.sleep(100)
                println("Hello ${Thread.currentThread().tid},  $i ")
            }
        }
        Thread.sleep(1000)
    }
}