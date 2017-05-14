nice -n 19 taskset -c 1-1 gradle runBench -Ptimeout=60000 -Pparallelism=1
nice -n 19 taskset -c 1-2 gradle runBench -Ptimeout=60000 -Pparallelism=2
nice -n 19 taskset -c 1-4 gradle runBench -Ptimeout=60000 -Pparallelism=4
nice -n 19 taskset -c 1-8 gradle runBench -Ptimeout=60000 -Pparallelism=8
nice -n 19 taskset -c 1-16 gradle runBench -Ptimeout=60000 -Pparallelism=16
nice -n 19 taskset -c 1-32 gradle runBench -Ptimeout=60000 -Pparallelism=32