


```sql
/* technical columns except for id (created_at, updated_at)
   were omitted. */

CREATE TABLE users (
  id UUID PRIMARY KEY,
  api_key TEXT,
  name TEXT,
  is_admin Boolean,
  UNIQUE (api_key),
  UNIQUE (name)
)

CREATE TABLE polls (
  id UUID PRIMARY KEY,
  user_id UUID, // fk
  question TEXT,
)

CREATE TABLE options (
  id UUID PRIMARY KEY,
  poll_id UUID, // fk
  option TEXT,
  rank INT,
  UNIQUE (poll_id, option)
)

CREATE TABLE votes (
  id UUID PRIMARY KEY,
  option_id UUID, // fk
  user_id UUID,   // fk
  UNIQUE (option_id, user_id)
)


SELECT 
  p.question,
  p.option,
  count(v.id)
FROM polls p
JOIN options o
ON p.id = o.poll_id
JOIN votes v
ON o.id = v.option_id
WHERE p.id = ?
```
