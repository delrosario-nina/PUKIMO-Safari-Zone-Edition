# Pukimo
## Creators
Christian Joseph Hernia

Nina Claudia Del Rosario

## Language Overview
PUKIMO Safari Zone Edition is a dynamically typed, Pokémon-themed domain-specific language (DSL) designed to simulate a Safari Zone adventure. Players explore the safari zone, encounter wild Pokémon, throw Safari Balls, and manage a temporary Safari team. Unlike the traditional Pokémon experience, this DSL emphasizes exploration, random encounters, and chance-based catching mechanics, creating a narrative, adventure-like experience.

Main Characteristics:
1. Simple object-oriented style with lightweight syntax.
2. Built-in types for SafariZone, Team, and Pokemon.
3. Declarative DSL-style commands (explore, throwBall, filter) for expressing game-like behavior.
4. Support for attributes like nature, behavior, friendliness, and shiny.
5. Human-readable syntax with comments (:>) for clarity and fun.

## Keywords

### Implemented Keywords
- **var** - declares a variable
- **true** - boolean literal
- **false** - boolean literal
- **null** - represents absence of a value
- **print** - outputs text or data to the console
- **SafariZone** - built-in type for managing Safari Zone state
- **Team** - built-in type for managing the player's caught Pokémon

### Reserved for Future Implementation (Parsed but Not Yet Executed)
- **if** - introduces a conditional block (not yet implemented)
- **else** - defines the alternative branch of an if statement (not yet implemented)
- **define** - declares a user-defined function (not yet implemented)
- **return** - returns a value from a function (not yet implemented)
- **explore** - special loop construct (not yet implemented)
- **run** - exits the current loop (not yet implemented)
- **throwBall** - attempts to catch a Pokémon (not yet implemented)
### Built-in Properties & Methods
These are predefined attributes and functions available on core objects (SafariZone, Team, Pokemon). They are not reserved words, but form part of the standard library.

### 1. SafariZone
**Constructor:** `SafariZone(balls: Int, turns: Int)`

**Properties:**
- `initialBalls` → initial number of Safari Balls (read-only)
- `initialTurns` → initial number of turns (read-only)
- `balls` → current Safari Balls remaining (read/write)
- `turns` → current turns remaining (read/write)
- `pokemonCount` → number of Pokemon in the zone (read-only)

**Properties (Collection):**
- `pokemon` → PokemonCollection object for managing Pokemon

**Methods (Resource Management):**
- `useBall()` → decrements balls by 1
- `useTurn()` → decrements turns by 1
- `reset()` → resets balls and turns to initial values
- `isGameOver()` → returns true if no balls or turns remaining

**Pokemon Collection Methods** (accessed via `.pokemon->`):
- `zone.pokemon->add(name)` → adds a Pokemon to the collection
- `zone.pokemon->remove(name)` → removes a Pokemon from the collection
- `zone.pokemon->list()` → lists all Pokemon as a string
- `zone.pokemon->find(name)` → finds Pokemon by name (case-insensitive)
- `zone.pokemon->random()` → returns random Pokemon from the collection
- `zone.pokemon->count()` → returns number of Pokemon in collection
- `zone.pokemon->clear()` → removes all Pokemon from collection
- `zone.pokemon->isEmpty()` → returns true if collection is empty

**Example:**
```
var zone = SafariZone(30, 500);
zone.pokemon->add("Pikachu");
zone.pokemon->add("Charmander");
print(zone.pokemon->list());
var encounter = zone.pokemon->random();
zone->useBall();
```

**Not Yet Implemented:**
- `refillBalls(amount)` → adds more Safari Balls (future)
- `refillTurns(amount)` → adds more turns (future)

### 2. Team
**Constructor:** `Team(trainerName: String, maxSize: Int = 6)`

**Properties:**
- `trainerName` → name of the trainer (read-only)
- `maxSize` → maximum team size (read-only, default: 6)
- `pokemonCount` → current number of Pokemon in team (read-only)

**Properties (Collection):**
- `pokemon` → PokemonCollection object for managing Pokemon

**Methods (Team Management):**
- `isFull()` → returns true if team is at max capacity
- `isEmpty()` → returns true if team has no Pokemon
- `has(name)` → checks if Pokemon is in team (case-insensitive)

**Pokemon Collection Methods** (accessed via `.pokemon->`):
- `team.pokemon->add(name)` → adds a Pokemon to the collection
- `team.pokemon->remove(name)` → removes a Pokemon from the collection
- `team.pokemon->list()` → lists all Pokemon as a string
- `team.pokemon->find(name)` → finds Pokemon by name (case-insensitive)
- `team.pokemon->random()` → returns random Pokemon from the collection
- `team.pokemon->count()` → returns number of Pokemon in collection
- `team.pokemon->clear()` → removes all Pokemon from collection
- `team.pokemon->isEmpty()` → returns true if collection is empty

**Example:**
```
var team = Team("Ash", 6);
team.pokemon->add("Pikachu");
team.pokemon->add("Charizard");
print(team.pokemon->list());
var full = team->isFull();
var hasPika = team->has("Pikachu");
```

**Not Yet Implemented:**
- `info(name, only=property)` → retrieves detailed info (future)

### 3. PokemonCollection

The `pokemon` property on SafariZone and Team objects returns a `PokemonCollection` that provides generic collection methods.

**Key Design Principle:**
- Use **`.` (dot)** for property access → `zone.pokemon`, `zone.balls`
- Use **`->` (arrow)** for method calls → `zone->useBall()`, `zone.pokemon->add("Pikachu")`

**Collection Methods:**
All collection methods use generic names (not Pokemon-specific):
- `add(item)` → adds an item to the collection
- `remove(item)` → removes an item from the collection
- `list()` → returns all items as a comma-separated string
- `find(item)` → finds an item by name (case-insensitive)
- `random()` → returns a random item from the collection
- `count()` → returns the number of items in the collection
- `clear()` → removes all items from the collection
- `isEmpty()` → returns true if the collection is empty

### 4. Pokemon Objects (Not Yet Implemented)

Individual Pokemon objects with properties are planned but not yet implemented:
- level → numeric level of the Pokémon
- shiny → boolean shiny status
- nature → string nature value
- behavior → string describing Pokémon behavior
- friendliness → numeric friendliness value
- caught → boolean whether caught

## Operators

### 1. Arithmetic Operators
- `+` → addition or string concatenation (supports mixed types: string + number, string + object)
- `-` → subtraction
- `*` → multiplication
- `/` → division
- `%` → modulo (remainder)

### 2. Comparison Operators
- `<` → less than
- `>` → greater than
- `==` → equal to
- `!=` → not equal to
- `>=` → greater than or equal to
- `<=` → less than or equal to

### 3. Logical Operators
- `&&` → logical conjunction (AND)
- `||` → logical disjunction (OR)
- `!` → logical negation (NOT)

### 4. Assignment Operators
- `=` → assigns a value to a variable or property

### 5. Access / Chaining Operators
- `->` → calling methods
- `.` → access a property of an object



## Literals
### 1. Numbers
Only integers are supported (no floats or decimals).
Used for counts, levels, turns, friendliness, etc.

Examples:
   ```
   myZone = SafariZone(10, 20);
   ```

### 2. Strings
Enclosed in double quotes " " for names, properties, or messages.

Examples:
   ```
   print("Welcome to the Safari Zone!");
   myZone->add("Charmander");
   ```

### 3. Booleans
Boolean literals: true or false.
Used for conditions and flags.

Examples:
   ```
   var isShiny = true;
   var caught = false;
   var gameOver = myZone->isGameOver();
   print(gameOver);
   ```

4. Null
   Represent the absence of a value.

   Example:
   ```
   encounter = null;
   ```

## Identifiers
### Rules for valid identifiers:
1. Must start with a letter (A-Z or a-z) or an underscore (_).
2. Can contain letters, digits (0-9), and underscores (_).
3. Cannot contain spaces or other special characters.
4. Cannot be a reserved keyword (e.g.,SafariZone, throwball, run, shiny, etc.).
5. Case-sensitive: Ash and ash are treated as different identifiers.


### Recommended Naming Style:
1. camelCase for variables and functions.
2. PascalCase for object-like structures or constants

Example:
```
var pikachuLevel = 15;
var trainerName = "Ash";
var moveName = "Thunderbolt";
```

### COMMENTS
1. :> - single line comments are written like this
   :> this is a comment
2. /* - multi-line comments are written like this
   /* this is a multiline
   comment */
3. Nested comments are not supported


### SYNTAX STYLE
1. Whitespace: Not significant, but indentation is recommended for readability.
2. Statement termination: Semicolons ; are required at the end of every statement.
   e.g.
   print("You caught Pikachu!");
3. Blocks: Use curly braces { } for grouping multiple statements (not yet implemented).
5. Instance method chaining: Use -> for calling object methods.
   e.g.
   ```
   var encounter = myZone->random();
   ```
7. Use . for class property access.
   e.g.
   ```
   myZone = SafariZone(10,10);
   print(myZone.balls);
   ```
9. Line breaks: Statements can be split across multiple lines for readability, but the semicolon must remain at the end.

## Sample Code
```
:>Using SafariZone Methods
myZone->useBall();
print("Balls remaining: " + myZone.balls);

myZone->useTurn();
print("Turns remaining: " + myZone.turns);

var gameOver = myZone->isGameOver();
print("Game Over: " + gameOver);

:>Managing Safari Zone Pokemon

myZone->reset();
print("Zone has been reset!");

var zone = SafariZone(30, 100);

zone.pokemon->add("Pikachu");
zone.pokemon->add("Charmander");
zone.pokemon->add("Bulbasaur");
zone.pokemon->add("Squirtle");

print("Pokemon in zone: " + zone.pokemon->list());
print("Zone has " + zone.pokemonCount + " Pokemon");

var encounter = zone.pokemon->random();
print("A wild " + encounter + " appeared!");

var found = zone.pokemon->find("Charmander");
print("Found: " + found);

:> Team Management
var team = Team("Ash", 6);

team.pokemon->add("Pikachu");
team.pokemon->add("Charizard");
team.pokemon->add("Bulbasaur");

print("Team size: " + team.pokemon->count());
print("All Pokemon: " + team.pokemon->list());

var hasPikachu = team->has("Pikachu");
print("Has Pikachu: " + hasPikachu);

var randomPokemon = team.pokemon->random();
print("Random Pokemon: " + randomPokemon);

var isFull = team->isFull();
print("Team is full: " + isFull);

:> String Concat
var pokemonCount = 5;
print("You have " + pokemonCount + " Pokemon!");

print("Balls left: " + myZone.balls);
print("Team size: " + myTeam.pokemon->count());

print("Zone: " + myZone);
print("Team: " + myTeam);
```
## Design Rationale
1. Single Trainer
    - The language assumes one implicit Trainer. This removes boilerplate and keeps code simple, like in the games (no need to declare a trainer each time).
2. Pokémon-flavored literals and booleans
    - Properties like shiny, nature, behavior, friendliness, and caught are built-in. This keeps the language close to the Pokémon world instead of generic programming terms.
3. Arrow-based method chaining (->)
    - Inspired by Laravel Eloquent ORM, looks cleaner to chain when compared to (.).
4. Static/Class-level access (.)
    - This cleanly separates global utilities from instance methods, while keeping the syntax consistent with property access.
5. English-like syntax and keyword
    - Core actions (e.g., throwBall, release, team, explore, run) are chosen to feel like Pokémon gameplay commands, making the language readable and fun.
6. Statements use semicolons
    - All statements end with ;. This makes it easier to track the end of line and to parse.
7. Minimalist control flow
    - Minimized to keep design simple and give the safari zone experience
8. Comments and readability
    - Fun comment syntax enhances engagement without affecting parsing.
9. Focus on clarity and accessibility
    - The syntax is designed to be easy to read for Pokémon fans, while also structured enough to be parsed like a real language.


# Grammar

### Main Body
**program**               --> stmtList

**stmtList**              --> {Stmt}

**stmt**                  --> NonIfStmt | IfStmt

### If statements
**ifStmt**                --> "if" "(" expr ")" "{" block "}" [elseBlock]

**elseBlock**               --> "else" "{" block "}"

### Non-if Statements
**nonIfStmt**             --> varDeclStmt
| exprStmt
| exploreStmt
| defineStmt
| printStmt
| throwBallStmt
| returnStmt
| runStmt
| block
| ";"

**varDeclStmt**           --> "var" IDENTIFIER "=" expr ";"

**exprStmt**              --> expr [";"]

**PrintStmt**             --> "print" Expr ";"

**ThrowBallStmt**         --> "throwBall" "(" Expr ")" ";"

**ReturnStmt**            --> "return" [Expr] ";"

**RunStmt**               --> "run" ";"

**DefineStmt**            --> "define" IDENTIFIER "(" paramList ")" Block

**ParamList**             --> IDENTIFIER ( "," IDENTIFIER )*

**Block**                 --> "{ StmtList "}"

**ExploreStmt**           --> "explore" "(" Expr ")" "{" Block "}"


### Precedence
expression            --> assignExpr

#### assignExpr is right-associative: a = b = c  => a = (b = c)
**assignExpr**            --> orExpr | [variableExpr|propertyAccessExpr] "=" assignExpr

**variableExpr**                --> identifier

**propertyAccessExpr**                --> PrimaryWithSuffixes "." IDENTIFIER

**callExpr** --> PrimaryWithSuffixes "(" OptArgList ")"
| PrimaryWithSuffixes "->" IDENTIFIER "(" OptArgList ")"


**orExpr**                --> AndExpr ( "||" AndExpr )*

**AndExpr**               --> EqualityExpr ( "&&" EqualityExpr )*

**EqualityExpr**          --> RelExpr ( ( "==" | "!=" ) RelExpr )*

**RelExpr**               --> AddExpr ( ( "<" | ">" | "<=" | ">=" ) AddExpr )*

**AddExpr**               --> MulExpr ( ( "+" | "-" ) MulExpr )*

**MulExpr**               --> UnaryExpr ( ( "*" | "/" | "%" ) UnaryExpr )*

**UnaryExpr**             --> ( "!" | "-" ) UnaryExpr | PostfixExpr

/* Postfix / high-precedence: calls, property access, method chaining (left-assoc) */
**PostfixExpr**           --> PrimaryWithSuffixes

/* PrimaryWithSuffixes: primary then zero or more suffixes (call, .prop, .method(args), ->method(args)) */
**PrimaryWithSuffixes**   --> Primary { Suffix }

**Suffix**                --> "." IDENTIFIER                    
| "." IDENTIFIER "(" OptArgList ")"
| "->" IDENTIFIER "(" OptArgList ")"
| "(" OptArgList ")"

### Primary Values
**Primary**               --> IDENTIFIER
| BUILTIN_CONSTRUCTOR_CALL
| NUMBER
| STRING
| "true"
| "false"
| "null"
| "(" Expression ")"


**BUILTIN_CONSTRUCTOR_CALL** --> "SafariZone"  "(" OptArgList ")"   
| "Team"  "(" OptArgList ")"

### Argument List
**OptArgList** --> ε | ArgList

**ArgList** --> Argument ( "," Argument )*

**Argument** --> NamedArg | PositionalArg

**NamedArg** --> IDENTIFIER "=" Expression

**PositionalArg** --> Expression

## Running PukiMO

### Compilation

Compile all source files into an executable JAR:
```powershell
$files = Get-ChildItem -Path src -Filter *.kt -Recurse | ForEach-Object { $_.FullName }
kotlinc @files -include-runtime -d PukiMO.jar
```

### Running Scripts

Execute a PukiMO script file:
```powershell
kotlin -cp PukiMO.jar MainKt <script-file>
```

Example:
```powershell
kotlin -cp PukiMO.jar MainKt safari.txt
```

### REPL Mode

Start the interactive REPL:
```powershell
kotlin -cp PukiMO.jar MainKt
```

## Test Suite

The language includes comprehensive test files:

- **simple.txt** - Basic language features
- **operators.txt** - All operators (arithmetic, comparison, logical)
- **variables.txt** - Variables and string operations
- **safari.txt** - Complete SafariZone object testing (14 tests)
- **team.txt** - Complete Team object testing (14 tests)
- **integration.txt** - Full Safari Zone adventure simulation

### Running Tests

Run a specific test:
```powershell
kotlin -cp PukiMO.jar MainKt <test-file>
```

Run all tests:
```powershell
.\run_tests.ps1
```

See **TEST_SUITE.md** for detailed test documentation.

