# Highly Configurable SQL

A Clojure library designed to manage your big SQL queries based on [YAML](https://yaml.org/).

## Version History

**0.2.0**
- Reworking `&` expansion to evaluate complex expressions
- SQL queries as functions support
- Remove templates support
- Add `__include` directive preprocessing

**0.1.0**
- Base HiCoSQL features: `&<name>` expansion, templates support


## Installation

### Leiningen

[![Clojars Project](https://img.shields.io/clojars/v/hicosql.svg)](https://clojars.org/hicosql)

## Description
1. Easy to read and write
2. Highly configurable
3. Composable queries
5. Designed to work with big complex SQL queries
6. SQL as data

### Why do I need it?

There are a lot of different SQL libraries. HugSQL, HoneySQL, YeSQL...

But unfortunately they provide good query composability only on Clojure level.

Then SQL code base is large, and you want to provide great readability, for example, for DBA's who
don't know Clojure you better need something like template language for your SQL than Clojure-like
solutions.  

So, the idea of this library is to manage SQL like configurations. It will help you if your queries
are really big, and they consist of repeatable parts.

### Usage
Each configuration file for HiCoSQL is a YAML file with keys and values that library expand.

For now the library supposed to be used with HugSQL.

This is an example HiCoSQL file with explaining comments.


```sql

#-- Comments start with # - for YAML and -- for SQL, so syntax highlighting would work no matter what
#-- extension .yaml or .sql you choose for a file. I prefer .sql.

#-- First of all, there are special engines directives, started with '__'
#-- This one includes the content of a file in current file.
#-- This is useful then you have some constants or queries that you use in different files
__include: 'base/common.sql'


#-- Some another constants which we will use below:
old: 60
young: 20

#-- Let's define our first query:
users: |
  SELECT id, first_name, last_name, age, address_id FROM users

#-- Now we can use it for defining another query. Here we use 'young' which will be expand to 20.
young_users: |
  &users WHERE age < &young

#-- Yet another query
old_users: |
  &users WHERE age > &old

#-- Now we can combine two previous queries:
old_and_young_users: |
  &young_users
  UNION
  &old_users

#-- Each query can use all queries defined above
mid_age_users: |
  SELECT * FROM users WHERE id NOT IN (SELECT id FROM ( &old_and_young_users ))

#-- In the real life we need to provide some values from Clojure code, for that we use :<name> strings
#-- which will be replaced with provided values. Let's define some queries.
query1: |
  SELECT * FROM (&users)
  WHERE age = :age AND salary = :salary AND address = :addr AND project = :project

query2: |
  SELECT max(salary) FROM users WHERE age = :age AND num = :num

#-- We can replace some of these parameters in next queries.
#-- Here we use query1 and query2 as functions calls with supplied parameters.
#-- The sign '!' on the end of query name means that we pass parameters in form:
#-- :key1 value1 :key2 value2 ... etc.
#-- We don't pass :project value because we suppose to use it from Clojure code.
query3: |
  SELECT * FROM &(query1! :age 20
                          :salary (query2! :age 40)
                          :num (* old young 30)
                          :addr "'Some address, Street 1'")
  UNION ALL
  SELECT * FROM some_other_table WHERE a_lot_of_conditions

#-- We can also use queries calls without additional keys before arguments,
#-- but for that we need to define query in function form to describe arguments order.
#-- Suppose we have:
q1(a, b, c, d): |
  SELECT :d, :c, :b, :a FROM some_table

#-- Now we can use it without additional keys. And in that case we don't use '!' at the end. Just 'q1'
q2: |
  SELECT some_sql FROM &(q1 "SQL expression" "'string with quotes'" 10 young)

#-- But of course, we can also use it in full form:
q3: |
  SELECT some_sql FROM &(q1! :a "SQL expression" :b "'string with quotes'" :c 10 :d young)

```

As you can see the library also supports some simple operations like: *, /, +, -, and other Clojure functions.
Such calls will be executed only once.
Notice, for now it resolves Clojure functions only if it is on the first position in the list, just
after the open bracket `(`.


You can put this code to YAML-to-JSON converter [here](https://yamlonline.com
),
[here](https://www.json2yaml.com/) or [here](https://codebeautify.org/yaml-to-json-xml-csv) and see what it really is.

It will be something like that:

```
{
	"__include": "base/common.sql",
	"old": 60,
	"young": 20,
	"users": "SELECT id, first_name, last_name, age, address_id FROM users\n",
	"young_users": "&users WHERE age < &young\n",
	"old_users": "&users WHERE age > &old\n",
	"old_and_young_users": "&young_users\nUNION\n&old_users\n",
	"mid_age_users": "SELECT * FROM users WHERE id NOT IN (SELECT id FROM ( &old_and_young_users ))\n",
	"query1": "SELECT * FROM (&users)\nWHERE age = :age AND salary = :salary AND address = :addr\n",
	"query2": "SELECT max(salary) FROM users WHERE age = :age AND num = :num\n",
	"query3": "SELECT * FROM &(query1! :age 20\n                        :salary (query2! :age 40)\n                        :num (* old young 30)\n                        :addr \"'Some address, Street 1'\")\nUNION ALL\nSELECT * FROM some_other_table WHERE a_lot_of_conditions\n",
	"q1(a, b, c, d)": "SELECT :d, :c, :b, :a FROM some_table\n",
	"q2": "SELECT some_sql FROM &(q1 \"SQL expression\" \"'string with quotes'\" 10 young)\n",
	"q3": "SELECT some_sql FROM &(q1! :a \"SQL expression\" :b \"'string with quotes'\" :c 10 :d young)\n"
}
```

That's exactly how the library see the file. All it does, is expansion and substitution.

Now let's see how to use it from Clojure.

```clojure 
(ns my-new-project.core
  (:require [hicosql.core :as hico]))
  
(hico/run-file "sql/test.sql")
```

The hico call above will produce such data:

```edn
#ordered/map([:const1 "'Common constant string. Notice, that it is in single quotes, so...'\n"]
             [:const2 "'... it will be inserted with them, like a string, not SQL expression'\n"]
             [:some_common_query "SELECT * FROM projects"]
             [:old 60]
             [:young 20]
             [:users "SELECT id, first_name, last_name, age, address_id FROM users\n"]
             [:young_users "SELECT id, first_name, last_name, age, address_id FROM users\n WHERE age < 20\n"]
             [:old_users "SELECT id, first_name, last_name, age, address_id FROM users\n WHERE age > 60\n"]
             [:old_and_young_users
              "SELECT id, first_name, last_name, age, address_id FROM users
                WHERE age < 20

               UNION
               SELECT id, first_name, last_name, age, address_id FROM users
                WHERE age > 60

               "]
             [:mid_age_users
              "SELECT * FROM users WHERE id NOT IN (SELECT id FROM ( SELECT id, first_name, last_name, age, address_id FROM users
                WHERE age < 20

               UNION
               SELECT id, first_name, last_name, age, address_id FROM users
                WHERE age > 60

                ))
               "]
             [:query1
              "SELECT * FROM (SELECT id, first_name, last_name, age, address_id FROM users
               )
               WHERE age = :age AND salary = :salary AND address = :addr
               "]
             [:query2 "SELECT max(salary) FROM users WHERE age = :age AND num = :num\n"]
             [:query3
              "SELECT * FROM SELECT * FROM (SELECT id, first_name, last_name, age, address_id FROM users
               )
               WHERE age = 20 AND salary = SELECT max(salary) FROM users WHERE age = 40 AND num = 36000
                AND address = 'Some address, Street 1'

               UNION ALL
               SELECT * FROM some_other_table WHERE a_lot_of_conditions
               "]
             [:q1 "SELECT :d, :c, :b, :a FROM some_table\n"]
             [:q2 "SELECT some_sql FROM SELECT 20, 10, 'string with quotes', SQL expression FROM some_table\n\n"]
             [:q3 "SELECT some_sql FROM SELECT 20, 10, 'string with quotes', SQL expression FROM some_table\n\n"])

```

`ordered-map` here is just a implementation of [ordered-map](https://github.com/flatland/ordered).
You can use it as an ordinary hash-map.

Here is how you can use it with your database:


```clojure 
(ns my-new-project.core
  (:require [hicosql.core :as hico]
            [hugsql.core :as hug]
            [clojure.jdbc :as jdbc]))
  

(def queries (hico/run-file "sql/test.sql")


;; hugsql/sqlvec-fn is a function what returns function from SQL string,
;; that creates sqlvec from the passed params
(defn make-request [db-spec query-key params]
  (jdbc/query 
    db-spec 
    ((hugsql/sqlvec-fn (-> queries query-key)) params))))

    
;; Now we're ready to make some requests
(make-request your-db-spec :users {}))

(make-request your-db-spec :query3 {:project "Some cool project"}))

(make-request your-db-spec :q1 {:a 1 :b 2 :c "C" :d "D"}))

(make-request your-db-spec :q3 {}))

```

It's just a simple example of usage. In production you'd rather want to write macros to create your
requests functions in compile-time.

I suppose you have understood the main idea of the library.

Feedback is appreciated.


## License
Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

ilevd © 2019
