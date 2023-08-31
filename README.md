# A template for Kotlin multiplatform projects that interoperate with Rust

This is a template for a Kotlin Multiplatform project, thoughtfully designed to seamlessly incorporate Rust within the codebase.

**_NOTE:_** For now only Kotlin/Native is supported.

## Structure

This project is structured in the following way:

-   The Rust code is managed using Cargo, and is placed in the `rustMain` directory.
    Note that this is not a Kotlin MP sourceSet. It is just a way to organize code for the better.
-   Every sourceSet can be used to store the code relative to the specific platform as usual.

Gradle is used as the main build tool for the whole project.
The Cargo commands that must be used for building the Rust library are encapsulated in some gradle tasks too.

### Interoperability in Kotlin/Native

Here's how it is possible to use Rust in Kotlin thanks to this configuration:

-   The `Cargo.toml` file is used to store the configuration of the Rust project.
    Pay attention to the line `crate_type = ["staticlib"]`. 
    This means that the Rust project does not contain a main entrypoint, but a collection of possible APIs.
-   Our Rust project is a library: the file `lib.rs` can expose some operations and structure. 
    In this case, just a simple `plus` method was implemented.
    The `#[no_mangle]` and `pub extern "C"` lines are important, but we won't explain their meaning in this README.
-   Kotlin is unable to directly use the Rust library,
    but we can create a header file (`.h`) using [cbindgen](https://github.com/mozilla/cbindgen) and our Rust code.
    cbindgen is, in fact, able to create C/C++11 headers for Rust libraries which expose a public C API.
    The `cbindgen.toml` file in the root contains some configuration for the tool.
-   The `nativeInterop` folder is used to automatically create a bridge between the Rust library and Kotlin Native.
    using cinterop, a `.def` file is used to specify some configuration for the C compiler and linker.
    Some configuration are also added in `build.gradle.kts`.
    Check [C Interoperability](https://kotlinlang.org/docs/native-c-interop.html) for more info.
-   A corresponding `plus` function is automatically created by Kotlin and can be used in the native sourceSet. 

## Building and running the example

The following command can be used to build the Rust library.
The header file will also be generated inside the `target` folder.
```shell
gradle cargoBuildRelease
```
It is then possible to launch the example with the command of you specific platform:
```shell
gradle runDebugExecutableLinuxX64 # Linux
gradle runDebugExecutableMacosX64 # MacOS
gradle runDebugExecutableMingwX64 # Windows
```

# Some Credits

A heartfelt acknowledgment goes to [FilippoVissani](https://github.com/FilippoVissani) for his passive but useful contribution.
Notably, he ingeniously crafted [an equivalent system in Scala](https://github.com/RustFields/scala-native-rust-interoperability-example),
a project that played a pivotal role in shaping this very example.

Started from [DanySK](https://github.com/DanySK/)'s [Template-for-Kotlin-Multiplatform-Projects
](https://github.com/DanySK/Template-for-Kotlin-Multiplatform-Projects) repository.