# SearchString - String Search in Java Files

Program for searching strings in files with extensions `.class`, `.properties` and `.jar` with recursive search and JAR archive support.

## Features

- Recursive search in directories
- Search in files with extensions: `.class`, `.properties`, `.jar`
- JAR archives unpacking in memory for searching inside them
- Case-insensitive search
- Detailed output with path information

## Compilation and Execution

### Compilation:
```bash
javac SearchString.java
```

### Execution:
```bash
java SearchString <search_path> <search_string>
```

### Usage Examples:

```bash
# Search in current directory
java SearchString . "myString"

# Search in specific directory
java SearchString C:\myproject "database"

# Search with spaces in string
java SearchString . "Hello World"
```

## Output Format

The program outputs:
- General search information
- Found files in real-time
- Final list of all found matches

For files inside JAR archives format: `jar_path -> path_inside_archive`

## Example Output

```
Searching for string: "database"
In directory: C:\myproject
Looking for files with extensions: .class, .properties, .jar
==========================================
Found in file: C:\myproject\config.properties
Found in JAR: C:\myproject\lib\myapp.jar -> com/example/Config.class
Found matches: 2
Results:
  C:\myproject\config.properties
  C:\myproject\lib\myapp.jar -> com/example/Config.class
```

## Requirements

- Java 11 or higher
- Read access to specified directory and files
