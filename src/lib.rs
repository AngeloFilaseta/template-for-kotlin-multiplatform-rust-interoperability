pub use plus::*;

pub mod plus {
    #[no_mangle]
    pub extern "C" fn plus(a: i32, b: i32) -> i32 {
        a + b
    }
}