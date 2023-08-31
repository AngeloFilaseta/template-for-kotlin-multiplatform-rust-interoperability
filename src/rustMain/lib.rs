pub use plus::*;

pub mod plus {
    #[no_mangle]
    pub extern "C" fn plus(a: i32, b: i32) -> i32 {
        a + b
    }
}

#[cfg(test)]
mod tests {
    use plus::plus;

    #[test]
    fn plus_test() {
        assert_eq!(0, plus(0, 0));
        assert_eq!(0, plus(-1, 1));
        assert_eq!(0, plus(1, -1));
        assert_eq!(4, plus(2, 2));
        assert_eq!(5, plus(3, 2));
        assert_eq!(5, plus(2, 3));
    }

}
