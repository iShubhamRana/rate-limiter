local curr_key = KEYS[1]
local prev_key = KEYS[2]

local limit  = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local now    = tonumber(ARGV[3])

-- Read both counters (default to 0 if missing)
local curr_count = tonumber(redis.call('GET', curr_key) or "0")
local prev_count = tonumber(redis.call('GET', prev_key) or "0")

local elapsed_in_current = now % window;
local prev_weight = (1 - elapsed_in_current / window);
local estimated =  math.floor(prev_count * prev_weight + curr_count);


if estimated >= limit then
    return {0,0}
end

-- set expiry after 2 windows
local new_count = redis.call('INCR' , curr_key)
if new_count == 1 then
    redis.call('PEXPIRE', curr_key , window * 2);
end


local remaining = limit - estimated - 1
return {1 , remaining}
