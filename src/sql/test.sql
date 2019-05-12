__include: 'base/common.sql'

old: 60
young: 20

include_test_query: SELECT * FROM d WHERE a = &const1 AND age = &old


users: SELECT id, first_name, last_name, age, address_id FROM users

young_users: |
  &users WHERE age < &young

old_users: |
  &users WHERE age > &old

old_and_young_users: |
  &young_users
  UNION
  &old_users

f(x, y, z): |
  SELECT :x, :y, :z FROM &users

f1: |
  SELECT &(f young 10 "20") FROM (&users)


ADDRESS: |
 'Some City, Some Street, 1'

ADDRESS2: |
 'Some other City, Some other Street, 2'

a1: |
  SELECT * FROM users WHERE age = :age AND salary = :salary;

a2(age, salary): |
  SELECT * FROM users WHERE age = :age AND salary = :salary AND dage > :date;

a2_age_salary: |
  SELECT * FROM users WHERE age = :age AND salary = :salary AND dage > :date;

b: |
  SELECT :age, :num FROM (&users)
  WHERE age = :age AND salary = :salary AND :d AND :other AND addr = :addr

b1: |
  age = :age + :age

c: |
  SELECT * FROM &(b!  :age 20
                      :d "WHERE age < 50"
                      :other "WHERE ageee < :age"
                      :age "NEW AGE"
                      :salary (b1! :age 40)
                      :num (* old young 30)
                      :addr ADDRESS)
  UNION ALL
  SELECT * FROM &(a2! :age 20)

#-- experimental testing
t1: |
  &(subs "hello world" 1 4)
  &(clojure.string/join "," [(clojure.string/replace "hello world" #"world" "br"), "zz"])
#--  PR = &(str "=" (cons + [1 2 3]))