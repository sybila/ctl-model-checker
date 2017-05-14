nice -n 19 taskset -c 1-1 gradle runBench -Ptimeout=30000 -Pparallelism=1 -Pmemory=4
nice -n 19 taskset -c 1-2 gradle runBench -Ptimeout=30000 -Pparallelism=2 -Pmemory=8
nice -n 19 taskset -c 1-4 gradle runBench -Ptimeout=30000 -Pparallelism=4 -Pmemory=16
nice -n 19 taskset -c 1-8 gradle runBench -Ptimeout=30000 -Pparallelism=8 -Pmemory=32
nice -n 19 taskset -c 1-16 gradle runBench -Ptimeout=30000 -Pparallelism=16 -Pmemory=64
nice -n 19 taskset -c 1-32 gradle runBench -Ptimeout=30000 -Pparallelism=32 -Pmemory=128