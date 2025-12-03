# Pukimo

## Creators

Christian Joseph Hernia  
Nina Claudia Del Rosario

## Language Overview

_PUKIMO Safari Zone Edition_ is a dynamically-typed, Pokémon-themed domain-specific language (DSL) for simulating a Safari Zone adventure. Players explore, encounter wild Pokémon, throw Safari Balls, and manage their team. The DSL uses declarative, narrative-style commands focused on exploration and chance-based catching.

### Main Characteristics

1. Simple object-oriented style with lightweight syntax.
2. Built-in types for SafariZone, Team, and Pokemon.
3. Declarative DSL-style commands (explore, throwBall, filter).
4. Attributes like nature, behavior, friendliness, and shiny.
5. Human-readable syntax and fun comments.

## Keywords & Built-in Constructs

### Implemented

- _var_ – Declares a variable
- _true_ / _false_ – Boolean literals
- _null_ – Absence of a value
- _print_ – Console output
- _SafariZone_ – Built-in game zone object
- _Team_ – Built-in object for caught Pokémon
- _define_ – Declares a user-defined function
- _return_ – Returns a value from a function
- _explore_ – Safari loop
- _run_ – Exits current loop
- _if/else_ – Conditional blocks

### Reserved / For Future:

- _throwBall_ – Attempts to catch a Pokémon (future)
- More advanced properties and methods (see below)

## Functions

### New Syntax: Type-Annotated Parameters

Functions must specify parameter types:

puki
define greet(name: string) {
print("Hello, " + name);
}

define add(a: int, b: int) {
print(a + b);
}

Valid parameter types:  
int, string, bool, double, SafariZone, Team, object, pokemon

## Built-in Functions

- _length(x)_ – Returns length of a string, size of a collection, or pokemonCount for SafariZone/Team.
  - Example: length("Pikachu") // 7
  - Example: length(team.trainerName)
  - Example: length(zone.pokemon)
- _readString()_ – Reads a line of input as string.
- _readInt()_ – Reads a line of input as integer.

_Note:_  
Use length(x) for string/collection length, not the arrow operator:
puki
length("Bulbasaur");
length(team.trainerName);
length(zone.pokemon);

## Types

- _int_ – Integer number
- _string_ – Text
- _bool_ – Boolean
- _double_ – Floating-point number (if enabled)
- _SafariZone_ – Zone object
- _Team_ – Team object
- _object_ – Generic object
- _pokemon_ – Pokémon object

## Properties & Methods

### 1. SafariZone

_Constructor:_  
SafariZone(balls: int, turns: int)

_Properties:_

- initialBalls, initialTurns (read-only)
- balls, turns
- pokemonCount (read-only)
- pokemon – a PokemonCollection

_Methods:_

- useBall(), useTurn(), reset(), isGameOver()
- Collection: add, remove, list, find, random, count, clear, isEmpty (via .pokemon->method())

_Example:_
puki
var zone = SafariZone(30, 500);
zone.pokemon->add("Pikachu");
print(zone.pokemon->list());
var encounter = zone.pokemon->random();
zone->useBall();

### 2. Team

_Constructor:_  
Team(trainerName: string, maxSize: int = 6)

_Properties:_

- trainerName, maxSize, pokemonCount, pokemon

_Methods:_

- isFull(), isEmpty(), has(name)
- Collection methods: add, remove, list, find, random, count, clear, isEmpty (via .pokemon->method())

_Example:_
puki
var team = Team("Ash", 6);
team.pokemon->add("Charizard");
print(team.pokemon->list());
var hasPika = team->has("Pikachu");

## Operators

- Arithmetic: +, -, \*, /, %
- Comparison: <, >, ==, !=, >=, <=
- Logical: &&, ||, !
- Assignment: =
- Method call: ->
- Property access: .

## Literals

- _int_: 123
- _string_: "Hello"
- _bool_: true / false
- _null_: null

## Identifiers

- Start: letter or \_
- Contain: letters, digits, \_
- Cannot be reserved keywords
- Case-sensitive

Recommended style:

- Variables/function: camelCase
- Objects/constants: PascalCase

## Comments

- Single line: :> this is a comment
- Multi-line: /_ ... _/
- No support for nested comments

## Syntax Summary

- Semicolons required: print("Hello!");
- Blocks with { ... } (for functions, conditionals, loops)
- Method calls: object->method(args);
- Property access: object.property
- Line breaks allowed, but semicolon must end statements

## Updated Grammar

(See _Parser.kt_ for full BNF.)

- _Function definition:_  
  define IDENTIFIER ( paramList ) Block

  - paramList → param (',' param)\*
  - param → IDENTIFIER : TYPE

- _Built-in call:_  
  length(expr)  
  readString()  
  readInt()

## Sample Code

puki
:> SafariZone methods
var zone = SafariZone(30, 100);
zone.pokemon->add("Pikachu");
zone->useBall();
print("Balls remaining: " + zone.balls);

:> Team management
var team = Team("Ash", 6);
team.pokemon->add("Charizard");
print("Team: " + team.pokemon->list());
print("Team size: " + length(team.pokemon)); // <== new built-in

:> Function usage
define shout(name: string) {
print("Wow! " + name + " appeared!");
}
shout("Mewtwo");

define teamSize(t: Team) {
print(length(t.pokemon));
}
teamSize(team);

:> Input
print("Enter your name:");
var trainer = readString();
print("Hello, " + trainer);

:> String length
print("Length: " + length(trainer));

## Error Handling

- Type mismatch errors in function calls.
- "Cannot call method on non-object type" if arrow operator (->) used on primitive types (e.g., string).
- Helpful runtime error messages for undefined variables, unauthorized assignments, etc.

## Running PukiMO

### Compilation

# Compile to executable JAR

kotlinc src/\*.kt -include-runtime -d PukiMO.jar

### Running Scripts

kotlin -cp PukiMO.jar MainKt <script-file>

# Example:

kotlin -cp PukiMO.jar MainKt safari.txt

### REPL Mode

kotlin -cp PukiMO.jar MainKt

## Test Suite

Includes files to test:

- Basic features: variables, print, arithmetic, logic
- All operators
- Object methods and properties
- Function definition/call with types
- Built-in functions: length, input
- Error cases (type mismatch, bad method call)
- Full Safari adventure

## Design Rationale

- Single implicit trainer simplifies code.
- Pokémon-flavored properties for immersion.
- Arrow operator for methods, dot for properties.
- Clear, English-like syntax.
- Required semicolons for easier parsing.
- Minimalist, readable control flow.
- Rich comments for engaging code.
