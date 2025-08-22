---

## Redis Data Management

- Keys in Redis are binary-safe strings, meaning they can contain any type of data including spaces, symbols, or even binary data.

- Values can be different data types like strings, hashes, lists, sets, sorted sets, bitmaps, hyperloglogs, and streams.

Set a key-value pair:
SET mykey "Hello, Redis!"

Get the value of a key:
GET mykey

Command to delete a key:
DEL mykey

Command to check if a key exists:
EXISTS mykey

Command to set a key with an expiration time (in seconds):
SET mykey "Hello, Redis!" EX 10

Command to set a timeout on an existing key:
EXPIRE mykey 10

Command to get the remaining time to live of a key:
TTL mykey

Command to remove the expiration from a key:
PERSIST mykey

Key Spaces
Redis keyspace is the collection of all keys stored in a Redis database.
You can switch between different key spaces (databases) using the SELECT command.
By default, Redis has 16 databases (numbered 0-15).

Command to switch to a different database:
SELECT 1

Key Naming

Keys in Redis are binary-safe strings, meaning they can contain any type of data including spaces, symbols, or even binary data.

Keep it simple but robust:
-> Use a consistent naming convention to make it easier to manage and query keys.

Avoid very short keys:
-> While shorter keys save memory, they can be ambiguous and less informative.

Use a schema based on your database design:
-> Define keys in a way that reflects your application's data structure.

SET employe:101:name "John"
SET employe:101:salary "5000"
SET employe:102:name "Alice"
SET employe:102:salary "6000"

Command to find keys that match a pattern:
KEYS employe:* ( not recommened, blocking command)

Use SCAN for better performance in production environments:
SCAN 2 MATCH employe:* COUNT 2

Command to get information about the keys in the current database:
INFO keyspace

Command to rename a key:
RENAME mykey newkey

Command to rename a key only if the new key does not exist:
RENAMENX mykey newkey

Command to delete a key asynchronously:
UNLINK mykey

Command to get the data type of a key:
TYPE mykey

---

## Redis data structures

1. Strings
2. Hashes
3. Lists
4. Sets
5. Sorted Sets
6. Bitmaps
7. HyperLogLogs
8. Streams
9. Geospatial Indexes

---

1. Strings

---

### 1. Setting Strings

**Set the string value of a key:**

```
SET key value
```

Example:

```
SET mykey "Hello"
```

**Set the value and expiration of a key:**

```
SETEX key seconds value
```

Example:

```
SETEX mykey 10 "Hello"
```

**Set the value of a key, only if the key does not exist:**

```
SETNX key value
```

Example:

```
SETNX mykey "Hello"
```

**Set multiple keys to multiple values:**

```
MSET key value [key value ...]
```

Example:

```
MSET key1 "Hello" key2 "World"
```

**Set multiple keys to multiple values, only if none of the keys exist:**

```
MSETNX key value [key value ...]
```

Example:

```
MSETNX key1 "Hello" key2 "World"
```

### 2. Getting Strings

**Get the value of a key:**

```
GET key
```

Example:

```
GET mykey
```

**Get the values of all the given keys:**

```
MGET key [key ...]
```

Example:

```
MGET key1 key2
```

### 3. Bit Operations

**Sets or clears the bit at offset in the string value stored at key:**

```
SETBIT key offset value
```

Example:

```
SETBIT mykey 7 1

```

**Returns the bit value at offset in the string value stored at key:**

```
GETBIT key offset
```

Example:

```
GETBIT mykey 7
```

**Count the number of set bits (population counting) in a string:**

```
BITCOUNT key [start end]
```

Example:

```
BITCOUNT mykey
```

**Perform a bitwise operation between multiple keys (AND, OR, XOR, NOT):**

```
BITOP operation destkey key [key ...]
```

Example:

```
BITOP AND destkey key1 key2
```

### 4. String Length

**Get the length of the value stored in a key:**

```
STRLEN key
```

Example:

```
STRLEN mykey
```

### 5. String Append

**Append a value to a key:**

```
APPEND key value
```

Example:

```
APPEND mykey " World"
```

### 6. Range Operations

**Get a substring of the string stored at a key:**

```
GETRANGE key start end
```

Example:

```
GETRANGE mykey 0 4
```

**Overwrite part of a string at key starting at the specified offset:**

```
SETRANGE key offset value
```

Example:

```
SETRANGE mykey 6 "Redis"
```

### 7. Increment and Decrement

**Increment the integer value of a key by one:**

```
INCR key
```

Example:

```
INCR mycounter
```

**Decrement the integer value of a key by one:**

```
DECR key
```

Example:

```
DECR mycounter
```

**Increment the integer value of a key by the given amount:**

```
INCRBY key increment
```

Example:

```
INCRBY mycounter 5
```

**Decrement the integer value of a key by the given amount:**

```
DECRBY key decrement
```

Example:

```
DECRBY mycounter 3
```

**Increment the float value of a key by the given amount:**

```
INCRBYFLOAT key increment
```

Example:

```
INCRBYFLOAT mycounter 1.5
```

### 8. Conditional Set

**Set the value of a key, only if the key does not exist:**

```
SETNX key value
```

Example:

```
SETNX mykey "Hello"
```

### 9. Setting with Expiration

**Set the value and expiration of a key:**

```
SETEX key seconds value
```

Example:

```
SETEX mykey 10 "Hello"
```

**Set the value and expiration of a key in milliseconds:**

```
PSETEX key milliseconds value
```

Example:

```
PSETEX mykey 10000 "Hello"
```

### 10. Multiple Keys

**Set multiple keys to multiple values:**

```
MSET key value [key value ...]
```

Example:

```
MSET key1 "Hello" key2 "World"
```

**Get the values of all the given keys:**

```
MGET key [key ...]
```

Example:

```
MGET key1 key2
```

**Set multiple keys to multiple values, only if none of the keys exist:**

```
MSETNX key value [key value ...]
```

Example:

```
MSETNX key1 "Hello" key2 "World"

```



### Memory ManagementTo check the memory usage of a specific key, you can use the following command:
MEMORY USAGE your:key

### Memory Management
Redis provides commands to manage memory usage effectively. Here are some key commands:
**Check the memory usage of the Redis server:**

```
INFO memory
```

**Check the memory usage of a specific key:**

```MEMORY USAGE key
```

**Check the memory fragmentation ratio:**

```
INFO memory




✅ 2. Safely Iterate Keys (Production-Safe)
```
SCAN cursor [MATCH pattern] [COUNT count]
```

Example:

```
SCAN 0 MATCH employe:* COUNT 10
```