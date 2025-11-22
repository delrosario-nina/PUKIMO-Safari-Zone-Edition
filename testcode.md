# Pukimo Language Test Script - Safari Zone Edition
# Copy and paste these examples line by line into the REPL
# NOTE: Only tests for currently implemented features

# ============================================
# Test 1: Basic Variable Declaration and Arithmetic
# ============================================

var x = 10;
print(x);

var y = 20;
print(x + y);

var z = x * y;
print(z);

var a = 100;
var b = 25;
print(a - b);
print(a / b);
print(a % b);

# ============================================
# Test 2: String Operations
# ============================================

var greeting = "Hello";
var name = "Trainer";
print(greeting + " " + name);

var pokemon = "Pikachu";
print("I choose you, " + pokemon + "!");

# ============================================
# Test 3: Boolean and Comparison
# ============================================

var isReady = true;
print(isReady);

print(10 > 5);
print(10 == 10);
print(5 != 10);
print(15 >= 10);
print(5 <= 10);

# ============================================
# Test 4: Logical Operators
# ============================================

print(true && true);
print(true && false);
print(false || true);
print(false || false);
print(!true);
print(!false);

var x = 10;
var y = 5;
print(x > 5 && y < 10);
print(x < 5 || y < 10);

# ============================================
# Test 5: Unary Operators
# ============================================

var num = 10;
print(-num);
print(-(-num));

var flag = true;
print(!flag);

# ============================================
# Test 6: Modulo Operator with Variables
# ============================================

var a = 10;
var b = 3;
print(a % b);

var remainder = 17 % 5;
print(remainder);

# ============================================
# Test 7: SafariZone Object Creation
# ============================================

var myZone = SafariZone(5, 10);
print(myZone);

# ============================================
# Test 8: SafariZone Properties
# ============================================

print(myZone.initialBalls);
print(myZone.initialTurns);
print(myZone.balls);
print(myZone.turns);
print(myZone.pokemonCount);

# ============================================
# Test 9: SafariZone Resource Management
# ============================================

myZone->useBall();
print(myZone.balls);

myZone->useTurn();
print(myZone.turns);

var gameOver = myZone->isGameOver();
print(gameOver);

myZone->reset();
print(myZone.balls);
print(myZone.turns);

# ============================================
# Test 10: SafariZone Pokemon Management
# ============================================

myZone.pokemon->add("Pikachu");
myZone.pokemon->add("Bulbasaur");
myZone.pokemon->add("Charmander");
print(myZone.pokemonCount);
print(myZone.pokemon->list());

var countZone = myZone.pokemon->count();
print(countZone);

var randomFromZone = myZone.pokemon->random();
print(randomFromZone);

var foundInZone = myZone.pokemon->find("bulbasaur");
print(foundInZone);

myZone.pokemon->remove("Bulbasaur");
print(myZone.pokemonCount);
print(myZone.pokemon->list());

# ============================================
# Test 11: Team Object Creation
# ============================================

var myTeam = Team("Ash");
print(myTeam);

var bigTeam = Team("Gary", 8);
print(bigTeam);

# ============================================
# Test 12: Team Properties
# ============================================

print(myTeam.trainerName);
print(myTeam.maxSize);
print(myTeam.pokemonCount);

# ============================================
# Test 13: Team Pokemon Management
# ============================================

myTeam.pokemon->add("Pikachu");
myTeam.pokemon->add("Charizard");
myTeam.pokemon->add("Blastoise");
print(myTeam.pokemonCount);
print(myTeam.pokemon->list());

var countTeam = myTeam.pokemon->count();
print(countTeam);

var teamEmpty = myTeam->isEmpty();
print(teamEmpty);

var teamFull = myTeam->isFull();
print(teamFull);

var hasPikachu = myTeam->has("Pikachu");
print(hasPikachu);

var hasMewtwo = myTeam->has("Mewtwo");
print(hasMewtwo);

var foundInTeam = myTeam.pokemon->find("charizard");
print(foundInTeam);

myTeam.pokemon->remove("Blastoise");
print(myTeam.pokemon->list());

# ============================================
# Test 14: Property Assignment
# ============================================

var zone = SafariZone(30, 500);
print(zone.balls);

zone.balls = 25;
print(zone.balls);

zone.turns = 450;
print(zone.turns);

# ============================================
# Test 15: Complex Safari Zone Simulation
# ============================================

var safari = SafariZone(3, 5);
safari.pokemon->add("Tauros");
safari.pokemon->add("Chansey");
safari.pokemon->add("Scyther");
safari.pokemon->add("Pinsir");

print("Welcome to the Safari Zone!");
print("You have " + safari.balls + " balls and " + safari.turns + " turns.");

safari->useBall();
safari->useTurn();
print("Balls left: " + safari.balls);
print("Turns left: " + safari.turns);

var encounter = safari.pokemon->random();
print("You encountered: " + encounter);

var safariGameOver = safari->isGameOver();
print("Game Over: " + safariGameOver);

# ============================================
# Test 16: Team Building Scenario
# ============================================

var trainer = Team("Red", 6);

trainer.pokemon->add("Pikachu");
print("Caught Pikachu!");

trainer.pokemon->add("Bulbasaur");
print("Caught Bulbasaur!");

trainer.pokemon->add("Charmander");
print("Caught Charmander!");

trainer.pokemon->add("Squirtle");
print("Caught Squirtle!");

trainer.pokemon->add("Pidgey");
print("Caught Pidgey!");

trainer.pokemon->add("Rattata");
print("Caught Rattata!");

print("Your team: " + trainer.pokemon->list());
print("Team size: " + trainer.pokemonCount + " / " + trainer.maxSize);

var canAddMore = trainer->isFull();
print("Team is full: " + canAddMore);

# ============================================
# Test 17: String Concatenation with Objects
# ============================================

var myZoneInfo = "Zone status: " + myZone;
print(myZoneInfo);

var teamInfo = "Team: " + myTeam;
print(teamInfo);

# ============================================
# Test 18: Multiple Assignments
# ============================================

var val1 = 10;
var val2 = 20;
var val3 = 30;

val1 = val2;
print(val1);

val2 = val3;
print(val2);

val3 = val1 + val2;
print(val3);

# ============================================
# END OF TESTS
# ============================================

print("All tests completed!");