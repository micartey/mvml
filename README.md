# mvml

<div align="center">
  <a href="https://www.oracle.com/java/">
    <img
      src="https://img.shields.io/badge/Written%20in-java-%23EF4041?style=for-the-badge"
      height="30"
    />
  </a>
  <a href="https://jitpack.io/#micartey/mvml/master-SNAPSHOT">
    <img
      src="https://img.shields.io/badge/jitpack-master-%2321f21?style=for-the-badge"
      height="30"
    />
  </a>
</div>

> [!IMPORTANT]\
> mvml is my bad try at somewhat reading and writing yaml files while preserving comments.
> This markup language is not according to yml specifications and I DO NOT recommend using it in production.


## 📚 Introduction

`mvml` (micarteys version of a markup language) is essentially a yaml clone with less features and without following the specification.
Yaml specification doesn't respect comments, thus breaking yaml files with comments when writing to it.
As this is very annoying and unwanted behaviour, I tried to create a parser which supports the yaml structure and respects comments.

### Usage

The following syntax is currently supported:

```yaml
# Some comment
# in multiple lines

my:
  # Some other comment
  field: 123

root-level-field: false
```

This is not much, but sufficient for most configurations.
If you want to use lists, you might want to represent them as arrays in a single line instead of a list as per usual.
The mvml parser gives you a string without trying to interpret it, altough this might be added in the future.


```java
MvmlConfiguration configuration = new MvmlConfiguration(file);
configuration.load();

String value = configuration.get("my.field"); // Retuns: 123
```