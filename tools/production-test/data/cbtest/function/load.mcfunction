scoreboard objectives add cbprobe dummy
craftingbomb selftest
craftingbomb status
execute store success score #r cbprobe run craftingbomb fuse 3
execute if score #r cbprobe matches 1 run say PASS_CRAFTINGBOMB_SELFTEST
execute unless score #r cbprobe matches 1 run say FAIL_CRAFTINGBOMB_SELFTEST
scoreboard objectives remove cbprobe