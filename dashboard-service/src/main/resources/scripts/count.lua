-- KEYS[1]=stats:offsets  KEYS[2]=stats:total  KEYS[3]=stats:min:<yyyyMMddHHmm>
-- ARGV[1]=topic:partition  ARGV[2]=offset  ARGV[3]=service:LEVEL
local last = redis.call('HGET', KEYS[1], ARGV[1])
if last and tonumber(ARGV[2]) <= tonumber(last) then
  return 0
end
redis.call('HINCRBY', KEYS[2], ARGV[3], 1)
redis.call('HINCRBY', KEYS[3], ARGV[3], 1)
redis.call('HSET', KEYS[1], ARGV[1], ARGV[2])
return 1