## Instructions
* Tested on Java 23.0.1<br/>
### How to run
To run just locally
```sh
clj -M -m polling-system-api.core [port]
```
Or alternatively, compile to bytecode and run
```sh
make build -B
java -jar ./target/polling_system_api.jar [port]
```
The port number can be passed as an optional argument to fix it.

### APIs
All the following endpoints assume you have a proper `Authorization` header value.<br/><br/>
Also, request body format should be `JSON` (not `edn`).<br/>

**Create poll**
```http
POST /api/poll
{
  "poll-id": "your-poll-id",
  "question": "your-question",
  "options": [
     "your options in an array",
     "another option"
   ]
}
```
It also returns the generated option-ids, which are supposed to be used for voting.<br/><br/>

Once you have poll-id, you can monitor the poll change (vote counts).<br/><br/>
**Get poll result**<br/>
The query parameter `wait-time-seconds` should be 0 to 30. If more than 30, internally reset to 30.<br/>
If omitted, the default value is 20.
```http
GET /api/poll/{your-poll-id}?wait-time-seconds=20
```

Or if `wait-time-seconds = 0`, you can immediately get the current result without waiting
```http
GET /api/poll/{your-poll-id}?wait-time-seconds=0
```

**Edit poll**
```http
PUT /api/poll/{your-poll-id}
{
  "question": "your-question",
  "options": [
     "your option in an array",
     "another option"
   ]
}
```
**Delete poll**
```http
DELETE /api/poll/{your-poll-id}
```
**Vote**
```http
POST /api/option/{your-option-id}
```
<br/>
<br/>

## About Design
The following features are **added** to the requirements:
- Only registered users can call each endpoint
- 1 vote per 1 user for the same option
  - If a user could vote the same option multiple times, it wouldn't make sense to me, so this rule was added. 
<br/>

### Data structure
In memory db stores data as follows:
```clojure
{:users  {"123adminkey" {:name "admin"
                         :admin? true}
          "123user1" {:name "user1"}
          "123user2" {:name "user2"}}

 :polls
 (atom {<poll-id> {:user-id <user-id>
                   :question <question>
                   :options 
                   {<option-id> {:vote-count <int>
                                 :option <option>
                                 :rank <int>}
                   :user-viewed ^ConcurrentLinkedQueue [<user-id> ...]
                    ...}}
         ...})

 :votes
 (atom {<option-id> {:poll-id <poll-id>                                
                     :queue ^ConcurrentLinkedQueue [<user-id> ...]}}
        ...}
)

```
- Why polls and votes are in the separated atoms?

By separating them, the vote(option) endpoint only requires `option-id`, but no need for `poll-id`.<br/>

- Why `ConcurrentLinkedQueue` instead of `atom`?

`atom(#{})` could also be used for the vote count in this schema. <br/>
`ConcurrentLinkedQueue` might outperform in concurrent high-traffic situations because its algorithm aims to reduce the amount of `compareAndSet` calls.<br/>
To support the feature *1 vote per 1 user for the same option* - storing users who voted is mandatory.<br/>
<br/>

### Long-Polling
I've chosen `long-polling` because the latency of real-time notification is not as critical for polling systems as it is for chatting apps.<br/>
It is simpler to implement and we have less server load with it.<br/>
The long polling is implemented by leveraging `core.async`'s `pub/sub` functions.<br/>
There were several options:<br/>
- checking the vote queue periodically
  - At the start of handling requests check vote counts and check changes in a loop 
  - Maybe the easiest but dirty. As users increase checking all counts for all options might be expensive to be repeated.
- logical clocks
  - Instead of checking vote count, have a serial number or some hash value (no need to `compareAndSet`) that changes on every voting.  
  - We would have to have them for each poll and need to create/remove (= assoc/dissoc) them with the polls' creation and removal
  - Still have to check the change in a loop
- Java `Object.wait()` and `Object.notifyAll()`
  - No need to check in a loop
  - There can be thread starvation and deadlocks, not working with virtual threads

`pub/sub` is a nice fit here and it allows to just block a thread to wait for a new change without loops.<br/>
Internally `async/pub` manages an atom with keys (= poll-id) and corresponding (`Mult`) channels and copies the source `Mult` channel (via `async/tap`) on `async/sub` for each request. <br/>
If Java version > 21, we can leverage virtual threads, and requests can be handled by other vthreads while a vthread is blocked without thread starvation.
 
<br/>
<br/>

### Potential future enhancements

- Single-selection polls

For real-world use cases, there should be such a type of poll as **single-selection poll** for which a user can vote for only one option. <br/>
Due to the time limit, I couldn't add this feature, and this version *only* supports **multi-selection polls**<br/>

I could implement that with the following structure:
```clojure
;;
;; !!! This is NOT implemented !!!
;;

{:users {} // omitted

 :polls
 (atom {<poll-id> {:user-id <user-id>
                   :type <single-selection, or multiple-selection>      // added
                   :voted-users (atom #{<user-id> ...})                 // added
                   :question <question>
                   :options 
                   {<option-id> {:vote-count <int>
                                 :option <option>
                                 :rank <int>}
                    ...}}
         ...})

 :votes {} // the same
        ...})

```
A (poll-)type should be added to determine whether voting should be restricted to one option.<br/>
The voted-users field is to support it, too.<br/>
The poll-id field can be added to the votes map for faster lookup of the target poll map.<br/>
<br/>

- Completed polls

For single-selection polls, after all users end voting, the poll result won't be changed, so we can just return the result without waiting.<br/>
During getting a poll result we could check the total votes number and compare it with the total users number. <br/>
If they are equal, store a flag that this poll won't be changed. Say, in `:completed?` key in the polls atom.<br/>

- Closing polls

Similarly, we could add such a feature to close polls by the creator of the poll or admins, so the vote result won't change after that.<br/>
Implementation is similar to the above, but we could name the key as `:closed?` and could share with the above feature. 

<br/>
<br/>

## Database schema

```sql
/*
   technical columns except for id (created_at, updated_at, etc)
   were omitted.
*/

CREATE TABLE users (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  is_admin Boolean NOT NULL,
  api_key TEXT NOT NULL,
  name TEXT NOT NULL,
  UNIQUE (api_key),
  UNIQUE (name)
)

CREATE TABLE polls (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users
  type SMALLINT NOT NULL,
  question TEXT NOT NULL
) 

CREATE TABLE options (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  poll_id UUID NOT NULL REFERENCES polls,
  rank INT NOT NULL,
  option TEXT NOT NULL,
  UNIQUE (poll_id, option)
)

CREATE TABLE votes (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users,
  poll_id UUID NOT NULL REFERENCES polls,
  option_id UUID NOT NULL REFERENCES options,
  option_id_unique_key UUID REFERENCES options, 
  UNIQUE (user_id, poll_id, option_id_unique_key)
)

CREATE TABLE votes_viewed (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users,
  --poll_id UUID NOT NULL REFERENCES polls,
  vote_id UUID NOT NULL REFERENCES votes,
  UNIQUE (user_id, vote_id)
)



```
<br/><br/>

### Considerations
This schema allows to vote with no locks (no conditional inserts).<br/>
`polls.type` is 0 for single-selection polls and 1 for multiple-selection polls.<br/>
The values in the `votes.option_id_unique_key` field should be `null` for single-selection polls and `option_id` for multiple-selection polls.<br/>
The unique constraint to `user_id`, `poll_id`, and `option_id_unique_key` prevents the violation of business rules.<br/>
The INSERT query will be as follows:
```sql
INSERT INTO votes (user_id, poll_id, opiton_id, option_id_unique_key)
SELECT
  ? --'user_id'
  ,? --'poll_id'
  ,? --'option_id'
  ,CASE WHEN p.type = 1 THEN o.id END
FROM options o
JOIN polls p
ON o.poll_id = p.id
WHERE o.id = ?
```
The SELECT query for poll change is as follows:
```sql
SELECT 
  p.question
 ,o.option
 ,o.rank
 ,count(v.id)
FROM polls p
JOIN options o
ON p.id = o.poll_id
JOIN votes v
ON o.id = v.option_id
WHERE p.id = ?
```

If options and votes tables have >1M rows, we can have btree indexes on options.poll_id and votes.option_id.<br/>
In this schema, poll_id is the only join key that all tables link to. On the flip side the records that have one poll_id can't have any relation with the records with the different poll_ids.<br/>
So in case there is much more data in the options and votes tables, we can partition them (+ the polls table, too) by poll_id (and even shard by it, too).
