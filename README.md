# Pukimo

## Creators

Christian Joseph Hernia  
Nina Claudia Del Rosario

## Language Overview

**PUKIMO Safari Zone Edition** is a dynamically-typed, Pokémon-themed domain-specific language (DSL) for simulating Safari Zone adventures. Players explore zones, encounter wild Pokémon, attempt catches with Safari Balls, and build their dream team. The language combines imperative programming with domain-specific constructs focused on exploration and chance-based gameplay.

### Main Characteristics

1. **Dynamic typing** with runtime type checking
2. **Object-oriented** Safari Zone and Team management
3. **Domain-specific** exploration with the `explore` statement
4. **First-class functions** with closures
5. **Interactive gameplay** with user input
6. **Probability-based** catching mechanics
7.  **Human-readable** syntax with Pokémon theming
    
---

## Keywords & Built-in Constructs

### Core Language

| Keyword | Purpose |
|---------|---------|
| `var` | Variable declaration |
| `define` | Function definition |
| `return` | Function return |
| `if` / `else` | Conditional execution |
| `while` | Loop statement |
| `for` / `in` / `to` | Range-based loop |
| `break` | Exit loop |
| `continue` | Skip to next iteration |
| `print` | Console output |
| `true` / `false` | Boolean literals |
| `null` | Null value |

### Safari Zone Specific

| Keyword | Purpose |
|---------|---------|
| `explore` | Iterate through Safari Zone encounters |
| `run` | Exit exploration early |
| `encounter` | Contextual variable for current Pokémon (only in `explore`) |
| `SafariZone` | Safari Zone constructor |
| `Team` | Trainer team constructor |

---

## Types

PukiMO is dynamically typed.  The following types exist at runtime:

- **int** – Integer numbers
- **string** – Text strings
- **bool** – `true` or `false`
- **null** – Absence of value
- **Array** – Mutable lists (created with `[...]`)
- **Function** – User-defined functions with closures
- **SafariZone** – Safari Zone game object
- **Team** – Pokémon team object
- **PokemonCollection** – Internal collection type

---

## Functions

### User-Defined Functions

```javascript
define greet(name) {
    print("Hello, " + name + "!");
}

define add(a, b) {
    return a + b;
}

:> Functions with closures
define makeCounter() {
    var count = 0;
    define increment() {
        count = count + 1;
        return count;
    }
    return increment;
}

var counter = makeCounter();
print(counter());  :> 1
print(counter());  :> 2
```

### Built-in Functions

| Function | Description | Example |
|----------|-------------|---------|
| `length(x)` | Returns length of string or array | `length("Pikachu")` → `7` |
| `readString()` | Reads user input as string | `var name = readString();` |
| `readInt()` | Reads user input as integer | `var age = readInt();` |
| `contains(array, item)` | Checks if array contains item | `contains(team, "Pikachu")` |
| `concat(arr1, arr2)` | Concatenates two arrays | `concat([1, 2], [3, 4])` |

---

## SafariZone Object

### Supported Constructor Declaration

```
var zone = SafariZone(balls=30, turns=500);
var zone = SafariZone(); 
var zone = SafariZone(10,10)
```

**Parameters:**
- `balls` – Number of Safari Balls
- `turns` – Number of turns allowed

### Properties

| Property | Type | Access | Description |
|----------|------|--------|-------------|
| `balls` | int | Read/Write | Current Safari Balls |
| `turns` | int | Read/Write | Current turns remaining |
| `initialBalls` | int | Read-only | Starting balls |
| `initialTurns` | int | Read-only | Starting turns |
| `pokemon` | PokemonCollection | Read-only | Pokémon in the zone |

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `useBall()` | void | Decrements balls by 1 |
| `useTurn()` | void | Decrements turns by 1 |
| `reset()` | void | Resets balls and turns to initial values |
| `isGameOver()` | bool | Returns true if balls or turns are 0 |

### Pokemon Collection Methods

Access via `zone.pokemon->method()`:

| Method | Parameters | Returns | Description |
|--------|-----------|---------|-------------|
| `add(name)` | string | void | Add a Pokémon |
| `addAll(array)` | array | void | Add multiple Pokémon |
| `remove(name)` | string | bool | Remove a Pokémon |
| `find(name)` | string | string/null | Find Pokémon by name |
| `random()` | - | string | Get random Pokémon |
| `list()` | - | array | Get all Pokémon as array |
| `count()` | - | int | Number of Pokémon |
| `isEmpty()` | - | bool | Check if empty |
| `clear()` | - | void | Remove all Pokémon |
| `setCatchRate(rate)` | int (0-100) | void | Set global catch rate % |
| `setSpeciesCatchRate(name, rate)` | string, int | void | Set species-specific catch rate % |
| `attemptCatch(name, zone)` | string, SafariZone | bool | Attempt to catch (consumes ball) |

### Example

```
var zone = SafariZone(balls=30, turns=500);

:> Add Pokémon to zone
zone.pokemon->add("Pikachu");
zone. pokemon->addAll(["Bulbasaur", "Charmander", "Squirtle"]);

:> Set catch rates
zone.pokemon->setCatchRate(50);  :> 50% global rate
zone.pokemon->setSpeciesCatchRate("Pikachu", 30);  :> 30% for Pikachu

:> Check contents
print("Pokémon in zone: " + zone.pokemon->count());
print("List: " + zone.pokemon->list());

:> Use resources
zone->useBall();
zone->useTurn();
print("Game over?  " + zone->isGameOver());
```

---

## Team Object

### Constructor

```javascript
var team = Team("Ash");  :> Default maxSize=6
var team = Team("Misty", maxSize=3);
```

**Parameters:**
- `trainerName` – Trainer's name (required)
- `maxSize` – Maximum team size (optional, default=6)

### Properties

| Property | Type | Access | Description |
|----------|------|--------|-------------|
| `trainerName` | string | Read-only | Trainer's name |
| `maxSize` | int | Read-only | Maximum team size |
| `pokemonCount` | int | Read-only | Current team size |
| `pokemon` | PokemonCollection | Read-only | Team's Pokémon |

### Methods

| Method | Parameters | Returns | Description |
|--------|-----------|---------|-------------|
| `isFull()` | - | bool | Check if team is full |
| `isEmpty()` | - | bool | Check if team is empty |
| `has(name)` | string | bool | Check if team has Pokémon |

### Pokemon Collection Methods

Access via `team. pokemon->method()`:

Same methods as SafariZone pokemon collection (add, remove, list, etc.)

### Example

```
var team = Team("Ash", maxSize=6);

:> Add Pokémon
team.pokemon->add("Pikachu");
team.pokemon->add("Charizard");

:> Check team
print("Team: " + team.pokemon->list());
print("Size: " + team.pokemonCount + "/" + team.maxSize);

if (team->isFull()) {
    print("Team is full!");
}

if (team->has("Pikachu")) {
    print("Pikachu is on the team!");
}
```

---

## Explore Statement

The `explore` statement iterates through Safari Zone encounters with a special contextual variable `encounter`.

### Syntax

```
explore(safariZone) {
    :> 'encounter' variable is automatically available here
    print("Wild " + encounter + " appeared!");
    
    :> Use 'run' to exit early
    if (encounter == "Mew") {
        run;
    }
}
```

### The `encounter` Variable

- **Contextual keyword** – Only valid inside `explore` blocks
- **Automatically bound** to current Pokémon name
- **Read-only** – Cannot be reassigned
- **Scoped** – Not accessible outside `explore`

### Example: Complete Safari

```
var zone = SafariZone(balls=30, turns=500);
var team = Team("Player", maxSize=6);

zone.pokemon->addAll(["Pidgey", "Rattata", "Pikachu", "Eevee"]);
zone.pokemon->setCatchRate(50);
zone.pokemon->setSpeciesCatchRate("Pikachu", 30);

explore(zone) {
    print("Encountered: " + encounter);
    
    :> Check if we have balls
    if (zone.balls > 0) {
        var caught = zone.pokemon->attemptCatch(encounter, zone);
        
        if (caught) {
            team. pokemon->add(encounter);
            print("Caught " + encounter + "!");
            
            :> Exit if team is full
            if (team. pokemonCount >= team.maxSize) {
                print("Team is full!");
                run;
            }
        }
    }
}

print("Final team: " + team.pokemon->list());
```

---

## Operators

### Arithmetic

| Operator | Description | Example |
|----------|-------------|---------|
| `+` | Addition / String concatenation | `5 + 3`, `"Hi" + "!"` |
| `-` | Subtraction | `10 - 4` |
| `*` | Multiplication | `6 * 7` |
| `/` | Division | `20 / 4` |
| `%` | Modulo | `17 % 5` |

### Comparison

| Operator | Description | Example |
|----------|-------------|---------|
| `==` | Equal | `x == 5` |
| `!=` | Not equal | `x != 5` |
| `<` | Less than | `x < 10` |
| `>` | Greater than | `x > 10` |
| `<=` | Less than or equal | `x <= 10` |
| `>=` | Greater than or equal | `x >= 10` |

### Logical

| Operator | Description | Example        |
|---------|--|----------------|
| `&&`    | Logical AND | `x > 5 && x < 10` |
| '\|\|'  | Logical OR | `x < 5 || x > 10` |
| `!`     | Logical NOT | `!isActive`    |

### Special

| Operator | Purpose | Usage |
|----------|---------|-------|
| `. ` | Property access | `zone.balls` |
| `->` | Method call | `zone->useBall()` |
| `=` | Assignment | `x = 5` |
| `[]` | Array access | `arr[0]` |

---

## Arrays

Arrays are mutable, dynamically-sized lists.

```javascript
:> Array literal
var team = ["Pikachu", "Bulbasaur", "Charmander"];

:> Access
print(team[0]);  :> "Pikachu"

:> Assignment
team[1] = "Ivysaur";

:> Length
print(length(team));  :> 3

:> Contains
if (contains(team, "Pikachu")) {
    print("Pikachu is here!");
}

:> Concatenation
var more = ["Squirtle", "Jigglypuff"];
var all = concat(team, more);
```

---

## Control Flow

### If/Else

```javascript
if (zone.balls > 10) {
    print("Plenty of balls!");
} else {
    print("Running low!");
}
```

### While Loop

```javascript
var i = 0;
while (i < 5) {
    print(i);
    i = i + 1;
}
```

### For Loop

```javascript
for (i in 0 to 9) {
    print("Count: " + i);
}

for (i in 1 to 10) {
    if (i % 2 == 0) {
        print(i + " is even");
    }
}
```

### Break and Continue

```javascript
for (i in 1 to 10) {
    if (i == 5) {
        break;  :> Exit loop
    }
    if (i % 2 == 0) {
        continue;  :> Skip even numbers
    }
    print(i);
}
```

---

## Comments

```javascript
:> Single-line comment

/* Multi-line
   comment */
```

---

## Identifiers

- **Start with:** letter or `_`
- **Contain:** letters, digits, `_`
- **Case-sensitive:** `myVar` ≠ `MyVar`
- **Cannot be keywords**

**Recommended Style:**
- Variables/functions: `camelCase`
- Constants/Objects: `PascalCase`

---

## Complete Example: Interactive Safari

```javascript
print("Welcome to Safari Zone!");
print("What is your name?");
var playerName = readString();

print("Choose difficulty (1=Easy, 2=Normal, 3=Hard):");
var difficulty = readInt();

var balls = 30;
var turns = 500;

if (difficulty == 1) {
    balls = 50;
    turns = 1000;
} else {
    if (difficulty == 3) {
        balls = 15;
        turns = 200;
    }
}

var zone = SafariZone(balls=balls, turns=turns);
var team = Team(playerName, maxSize=6);

zone.pokemon->addAll(["Pidgey", "Rattata", "Pikachu", "Eevee", "Dratini", "Articuno"]);
zone.pokemon->setCatchRate(50);
zone.pokemon->setSpeciesCatchRate("Articuno", 3);

var encounterCount = 0;

explore(zone) {
    encounterCount = encounterCount + 1;
    print("Wild " + encounter + " appeared!");
    
    if (zone.balls > 0) {
        var caught = zone.pokemon->attemptCatch(encounter, zone);
        
        if (caught) {
            team.pokemon->add(encounter);
            print("Caught " + encounter + "!");
            
            if (team.pokemonCount >= team.maxSize) {
                print("Team is full!");
                run;
            }
        } else {
            print(encounter + " escaped!");
        }
    }
}

print("");
print("=== Adventure Complete ===");
print("Encounters: " + encounterCount);
print("Final Team: " + team.pokemon->list());
print("Thanks for playing, " + playerName + "!");
```

---

## Running PukiMO

### Compilation

```bash
kotlinc src/*. kt -include-runtime -d PukiMO.jar
```

### Run Script

```bash
java -jar PukiMo.jar examples/test_pukimo.txt
```

---

## Error Handling

PukiMO provides clear, helpful error messages:

- **Type errors:** "Cannot call method on non-object type"
- **Undefined variables:** "Undefined variable 'x'" with hint "Did you forget to declare with 'var'?"
- **Arity errors:** "Expected 2 arguments but got 1"
- **Array errors:** "Array index 5 out of bounds (size 3)"
- **Game errors:** "No Safari Balls remaining!"

---

## Design Rationale

1. **Contextual `encounter` keyword** – Clean syntax for exploration
2. **Arrow operator** (`->`) – Distinguishes methods from properties
3. **Implicit probability** – Catch mechanics hidden for simplicity
4. **Closures** – Enable advanced patterns while staying simple
5. **Domain-specific** – Safari Zone concepts are first-class
6. **Interactive** – Built-in input for engaging gameplay
7. **Pokémon theming** – Fun, immersive language for fans


## Context-Free Grammar (CFG)

### Notation

- `→` : Production rule
- `|` : Alternative (OR)
- `*` : Zero or more
- `+` : One or more
- `?` : Optional (zero or one)
- `ε` : Empty production
- `TERMINAL` : Terminal symbols (uppercase)
- `nonTerminal` : Non-terminal symbols (camelCase)

---

### Program Structure

```
program           → statement* EOF

statement         → ifStmt
                  | nonIfStmt

nonIfStmt         → varDecl
                  | printStmt
                  | runStmt
                  | whileStmt
                  | forStmt
                  | breakStmt
                  | continueStmt
                  | exploreStmt
                  | returnStmt
                  | defineStmt
                  | block
                  | exprStmt

block             → "{" blockStatements "}"

blockStatements   → statement*
```

---

### Declarations & Definitions

```
varDecl           → "var" IDENTIFIER ("=" expression)?  ";"

defineStmt        → "define" IDENTIFIER parameterList block

parameterList     → "(" (IDENTIFIER ("," IDENTIFIER)*)? ")"
```

---

### Statements

```
printStmt         → "print" "(" expression ")" ";"

exprStmt          → expression ";"? 

ifStmt            → "if" "(" expression ")" block ("else" block)?

whileStmt         → "while" "(" expression ")" block

forStmt           → "for" "(" IDENTIFIER "in" expression "to" expression ")" block

breakStmt         → "break" ";"

continueStmt      → "continue" ";"

returnStmt        → "return" expression?  ";"

exploreStmt       → "explore" "(" IDENTIFIER ")" block

runStmt           → "run" ";"
```

---

### Expressions

```
expression        → assignExpr

assignExpr        → orExpr ("=" assignExpr)?

orExpr            → andExpr ("||" andExpr)*

andExpr           → equalityExpr ("&&" equalityExpr)*

equalityExpr      → relationalExpr (("==" | "!=") relationalExpr)*

relationalExpr    → additiveExpr (("<" | "<=" | ">" | ">=") additiveExpr)*

additiveExpr      → multiplicativeExpr (("+" | "-") multiplicativeExpr)*

multiplicativeExpr → unaryExpr (("*" | "/" | "%") unaryExpr)*

unaryExpr         → ("!" | "-") unaryExpr
                  | primaryWithSuffixes

primaryWithSuffixes → primary suffix*

suffix            → "." IDENTIFIER                       // Property access
                  | "->" IDENTIFIER "(" argumentList ")" // Method call
                  | "[" expression "]"                   // Array access
                  | "(" argumentList ")"                 // Function call
```

---

### Primary Expressions

```
primary           → NUMERIC_LITERAL
                  | STRING_LITERAL
                  | "true"
                  | "false"
                  | "null"
                  | IDENTIFIER
                  | constructorCall
                  | arrayLiteral
                  | "(" expression ")"

constructorCall   → ("SafariZone" | "Team") "(" argumentList ")"

arrayLiteral      → "[" (expression ("," expression)*)? "]"
```

---

### Arguments

```
argumentList      → ")" 
                  | argument ("," argument)* ")"

argument          → namedArgument
                  | expression

namedArgument     → IDENTIFIER "=" expression
```

**Note:** The parser checks for named arguments by looking ahead: if `IDENTIFIER` is followed by `=`, it's a named argument.

---

### Lexical Grammar

```
NUMERIC_LITERAL   → DIGIT+

STRING_LITERAL    → '"' CHAR* '"'

IDENTIFIER        → (LETTER | "_") (LETTER | DIGIT | "_")*

LETTER            → [a-zA-Z]

DIGIT             → [0-9]

CHAR              → any Unicode character except '"' and '\'
                  | ESCAPE_SEQUENCE

ESCAPE_SEQUENCE   → "\n" | "\t" | "\"" | "\\"
```

---

### Keywords (Reserved)

```
KEYWORD           → "var" | "define" | "return" 
                  | "if" | "else"
                  | "while" | "for" | "in" | "to"
                  | "break" | "continue" 
                  | "print"
                  | "true" | "false" | "null"
                  | "explore" | "run"
                  | "SafariZone" | "Team"
```

---

### Tokens (Terminal Symbols)

```
// Literals
NUMERIC_LITERAL
STRING_LITERAL
BOOLEAN_LITERAL    → "true" | "false"
NULL_LITERAL       → "null"

// Keywords
VAR_KEYWORD        → "var"
DEFINE_KEYWORD     → "define"
RETURN_KEYWORD     → "return"
IF_KEYWORD         → "if"
ELSE_KEYWORD       → "else"
WHILE_KEYWORD      → "while"
FOR_KEYWORD        → "for"
IN_KEYWORD         → "in"
TO_KEYWORD         → "to"
BREAK_KEYWORD      → "break"
CONTINUE_KEYWORD   → "continue"
PRINT_KEYWORD      → "print"
EXPLORE_KEYWORD    → "explore"
RUN_KEYWORD        → "run"
SAFARI_ZONE        → "SafariZone"
TEAM               → "Team"

// Operators
ASSIGN             → "="
PLUS               → "+"
MINUS              → "-"
MULTIPLY           → "*"
DIVIDE             → "/"
MODULO             → "%"
EQUAL_EQUAL        → "=="
NOT_EQUAL          → "!="
LESS_THAN          → "<"
LESS_EQUAL         → "<="
GREATER_THAN       → ">"
GREATER_EQUAL      → ">="
AND                → "&&"
OR                 → "||"
NOT                → "!"
DOT                → "."
ARROW              → "->"

// Delimiters
LEFT_PAREN         → "("
RIGHT_PAREN        → ")"
LEFT_BRACE         → "{"
RIGHT_BRACE        → "}"
LEFT_BRACKET       → "["
RIGHT_BRACKET      → "]"
SEMICOLON          → ";"
COMMA              → ","

// Special
IDENTIFIER
EOF
```

---

### Contextual Keywords

```
CONTEXTUAL         → "encounter"  (only valid inside explore blocks)
```

**Note:** `encounter` is treated as a regular `IDENTIFIER` by the lexer, but has special semantics inside `explore` blocks (validated during evaluation, not parsing).

---

### Operator Precedence (Lowest to Highest)

```
1. Assignment          =
2. Logical OR          ||
3. Logical AND         &&
4. Equality            == !=
5. Relational          < <= > >=
6. Additive            + -
7.  Multiplicative      * / %
8. Unary               !  -
9.  Suffix              . -> [] ()
```

---

### Associativity

- **Right-associative:** Assignment (`=`), Unary operators (`!`, `-`)
- **Left-associative:** All other binary operators

---

### Grammar Notes

#### 1. **Statement Disambiguation**

The parser distinguishes `ifStmt` from other statements at the top level:

```
statement → ifStmt | nonIfStmt
```

This ensures proper handling of if-else chains.

#### 2. **Suffix Parsing**

The parser uses a loop to handle multiple suffixes:

```kotlin
while (true) {
    when {
        DOT        → parsePropertyAccess()
        ARROW      → parseMethodCall()
        LEFT_BRACKET → parseArrayAccess()
        LEFT_PAREN → parseFunctionCall()
        else       → break
    }
}
```

This allows chains like:
```javascript
zone.pokemon->list()[0]
team->isFull()
```

#### 3.  **Named Arguments**

Named arguments are detected by lookahead:

```
isNamedArgument() → IDENTIFIER followed by "="
```

Examples:
```javascript
SafariZone(balls=30, turns=500)
Team("Ash", maxSize=6)
```

#### 4.  **Optional Semicolons in Expression Statements**

```
exprStmt → expression ";"?
```

The semicolon is optional for expression statements (checked but not required).

#### 5. **Constructor Recognition**

Constructors are recognized by specific tokens:

```
primary → ...  | constructorCall

constructorCall → ("SafariZone" | "Team") "(" argumentList ")"
```

---

### Special Productions

#### SafariZone Constructor

```javascript
SafariZone()                          // Empty args
SafariZone(30, 500)                   // Positional
SafariZone(balls=30, turns=500)       // Named
SafariZone(turns=500, balls=30)       // Named (order doesn't matter)
```

**Grammar:**
```
constructorCall → "SafariZone" "(" argumentList ")"
argumentList    → (positional | named) ("," (positional | named))* ")"
```

#### Team Constructor

```javascript
Team("Ash")                           // Required positional
Team("Ash", maxSize=6)                // Positional + named
```

#### Explore Statement

```javascript
explore(zoneName) {
    // 'encounter' is implicitly available
    print(encounter);
}
```

**Grammar:**
```
exploreStmt → "explore" "(" IDENTIFIER ")" block
```

---

### Semantic Constraints (Not Enforced by CFG)

These are validated during parsing or evaluation:

1. **Variables must be declared before use**
2.  **`return` only valid inside functions**
3. **`break` and `continue` only valid inside loops**
4. **`run` only valid inside `explore` blocks**
5. **`encounter` only valid inside `explore` blocks** (read-only)
6. **Cannot assign to read-only identifiers** (`encounter`)
7. **Cannot use reserved words as variable names** (`encounter` in certain contexts)
8. **Property access (`.`) vs method calls (`->`)**:
    - `. ` must NOT be followed by `(`
    - `->` must BE followed by `(`
9. **Array indices must be integers** (runtime check)
10. **Constructor names are special-cased** (`SafariZone`, `Team`)

---

### Error Recovery

The parser implements **synchronization** on errors:

1. **Skip to statement boundary** (`;`)
2. **Skip to statement keyword** (`var`, `if`, `while`, etc.)
3. **Add contextual hints** for unclosed delimiters (`(`, `{`)

---

### Extended BNF (EBNF) Summary

```ebnf
program    ::= statement* EOF
statement  ::= ifStmt | nonIfStmt
nonIfStmt  ::= varDecl | printStmt | runStmt | whileStmt | forStmt 
             | breakStmt | continueStmt | exploreStmt | returnStmt 
             | defineStmt | block | exprStmt

varDecl    ::= "var" IDENTIFIER ("=" expression)? ";"
printStmt  ::= "print" "(" expression ")" ";"
ifStmt     ::= "if" "(" expression ")" block ("else" block)?
whileStmt  ::= "while" "(" expression ")" block
forStmt    ::= "for" "(" IDENTIFIER "in" expression "to" expression ")" block
defineStmt ::= "define" IDENTIFIER "(" (IDENTIFIER ("," IDENTIFIER)*)?  ")" block
returnStmt ::= "return" expression? ";"
exploreStmt::= "explore" "(" IDENTIFIER ")" block
runStmt    ::= "run" ";"
breakStmt  ::= "break" ";"
continueStmt ::= "continue" ";"
exprStmt   ::= expression ";"? 

expression ::= assignExpr
assignExpr ::= orExpr ("=" assignExpr)?
orExpr     ::= andExpr ("||" andExpr)*
andExpr    ::= equalityExpr ("&&" equalityExpr)*
equalityExpr ::= relationalExpr (("==" | "!=") relationalExpr)*
relationalExpr ::= additiveExpr (("<" | "<=" | ">" | ">=") additiveExpr)*
additiveExpr ::= multiplicativeExpr (("+" | "-") multiplicativeExpr)*
multiplicativeExpr ::= unaryExpr (("*" | "/" | "%") unaryExpr)*
unaryExpr  ::= ("!" | "-") unaryExpr | primaryWithSuffixes
primaryWithSuffixes ::= primary suffix*
suffix     ::= "." IDENTIFIER 
             | "->" IDENTIFIER "(" argumentList ")"
             | "[" expression "]" 
             | "(" argumentList ")"

primary    ::= NUMERIC_LITERAL | STRING_LITERAL | "true" | "false" | "null"
             | IDENTIFIER | constructorCall | arrayLiteral | "(" expression ")"
constructorCall ::= ("SafariZone" | "Team") "(" argumentList ")"
arrayLiteral ::= "[" (expression ("," expression)*)? "]"
argumentList ::= (argument ("," argument)*)? ")"
argument   ::= IDENTIFIER "=" expression | expression
```

---

### Grammar Ambiguities and Resolutions

#### 1. **Dangling Else** 

```javascript
if (x > 0)
    if (y > 0)
        print("both");
else  // Binds to inner if
    print("not both");
```

**Resolution:** Else binds to nearest if (standard approach).

#### 2. **Property vs Method** 

```javascript
zone.balls       
zone->useBall()  
zone.useBall()   
zone->balls    
```

**Resolution:** Parser enforces syntax rules during parsing.

## License

**Educational Use Only**

This project was created as an academic assignment for a programming language course.

- Free for **educational purposes** (learning, teaching, academic research)
- Free for **personal, non-commercial use**


**Copyright © 2025 Christian Joseph Hernia and Nina Claudia Del Rosario**

---

