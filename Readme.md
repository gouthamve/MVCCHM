# Concurrent HashMap using the principles of MVCC

A Hashmap implementation for usecases with high Read/Write Ratio.

## Usecase and Existing implementations

### Usecase
We were targeting a very high read/write ratio (~99%) which is common in caches where there are a very high amounts of reads.

Now the biggest issue with the existing implementations is that reads and writes block each other.

### Existing Implementations
Almost all the existing implementations use a granular lock where there are **n** locks protecting the **n** slices of the map.

If there is heavy read activity, the write throughput would be low as the writers are blocked.


## MVCC HashMap

### MVCC: Multiversion Concurrency Control
This is concurrency control mechanism where the reads and writes do not block each other. And the reads do not block each other. Ain't it perfect?

This works by never modifying an element (hence no contention) and only appends. Any appending is done via atomic CAS hence the implementation is lock-free for reads.

For example, the backing store for the HashMap is an array. Now we don't modify the elements. It will be pointing to a linked list. And we append new elements to the Linked List sorted by version. The appending is done via CAS.

For the reads, we read a snapshot of the HashMap that existed at the beginning of the read call. Any changes that are made to the map during the function call are not visible to the read call.


### The Lock-Free LinkedList
Now each element of the backing array is a Harris Lock-Free Linked List. Implemented using AtomicMarkedReference from the Java.util.concurrent package.

Elements in this list are sorted high to low with the head having the highest version. We store the KV pairs inside the node but the comparision is done on the version. Here the version is the write transaction number.

### The HashMap
This is a cuckoo hashmap implementation. We chose Cuckoo as it gives a strict read performance bounds and read performance are what we are looking for primarily.

## The Write Method
Writes are sequential in our implementation. There is a lock protecting each write.

We just find the index to insert at and add the element with the version to the Linked List there. If we need to displace an element, we add the element and then go on to insert the displaced element. Finally, we commit the change by updating the lastSuccessfulTxn variable. If we don't commit, the reads won't pick this up.

If we are not able to insert, we rollback our changes and throw an error. Ideally, we need to grow and rehash the table.

## The Read Method
Here comes the interesting part. We read the lastSuccessfulTxn at the beginning. We use this to only read the elements that existed at the lastSuccessfulTxn. While reading an element at an index, if a new element is added (equivalent of update of the element at that index), we ignore the new element and still read the older element.

This snapshotting allows the readers to be non-blocking.


## The comparision

In our tests we saw that the MVCC version consistently outperformed the Granular locked version when the readers:writers ratio was `>99%`. This is both in the read and write throughput.

### Test Setup

We are inserting 128000 keys into the map using `n` writer threads while `1000-n` reader threads are doing random reads.

We beat the default implementation when the readers/writers are >= 99%
### Graph1: Time taken for 128000 elements to be inserted.
![](http://i.imgur.com/4gibxQl.png)

```
CUCKOO WRITERS: 999 TIME: 726
CHM WRITERS: 999 TIME: 240
CUCKOO WRITERS: 990 TIME: 223
CHM WRITERS: 990 TIME: 208
CUCKOO WRITERS: 950 TIME: 104
CHM WRITERS: 950 TIME: 96
CUCKOO WRITERS: 900 TIME: 232
CHM WRITERS: 900 TIME: 66
CUCKOO WRITERS: 800 TIME: 60
CHM WRITERS: 800 TIME: 63
CUCKOO WRITERS: 700 TIME: 76
CHM WRITERS: 700 TIME: 54
CUCKOO WRITERS: 600 TIME: 55
CHM WRITERS: 600 TIME: 56
CUCKOO WRITERS: 500 TIME: 278
CHM WRITERS: 500 TIME: 45
CUCKOO WRITERS: 400 TIME: 60
CHM WRITERS: 400 TIME: 47
CUCKOO WRITERS: 300 TIME: 33
CHM WRITERS: 300 TIME: 49
CUCKOO WRITERS: 200 TIME: 24
CHM WRITERS: 200 TIME: 31
CUCKOO WRITERS: 100 TIME: 20
CHM WRITERS: 100 TIME: 43
CUCKOO WRITERS: 50 TIME: 12
CHM WRITERS: 50 TIME: 100
CUCKOO WRITERS: 10 TIME: 9
CHM WRITERS: 10 TIME: 17
CUCKOO WRITERS: 1 TIME: 8
CHM WRITERS: 1 TIME: 14
```

We consistently beat the read throughput of the default implementation.
### Graph2: The reads/sec.
![](http://i.imgur.com/KH2HlAM.png)

```
CUCKOO WRITERS: 999 TIME: 502
CUCKOO READS: 4626244
CHM WRITERS: 999 TIME: 166
CHM READS: 4248646
CUCKOO WRITERS: 990 TIME: 86
CUCKOO READS: 4569072
CHM WRITERS: 990 TIME: 82
CHM READS: 2996850
CUCKOO WRITERS: 950 TIME: 253
CUCKOO READS: 3296084
CHM WRITERS: 950 TIME: 264
CHM READS: 2517714
CUCKOO WRITERS: 900 TIME: 303
CUCKOO READS: 4747657
CHM WRITERS: 900 TIME: 73
CHM READS: 2331734
CUCKOO WRITERS: 800 TIME: 77
CUCKOO READS: 4909932
CHM WRITERS: 800 TIME: 61
CHM READS: 1164375
CUCKOO WRITERS: 700 TIME: 67
CUCKOO READS: 5442085
CHM WRITERS: 700 TIME: 78
CHM READS: 3837411
CUCKOO WRITERS: 600 TIME: 218
CUCKOO READS: 5539070
CHM WRITERS: 600 TIME: 137
CHM READS: 2859265
CUCKOO WRITERS: 500 TIME: 48
CUCKOO READS: 5405927
CHM WRITERS: 500 TIME: 41
CHM READS: 1572553
CUCKOO WRITERS: 400 TIME: 34
CUCKOO READS: 5223380
CHM WRITERS: 400 TIME: 39
CHM READS: 1556175
CUCKOO WRITERS: 300 TIME: 148
CUCKOO READS: 5888755
CHM WRITERS: 300 TIME: 184
CHM READS: 1776627
CUCKOO WRITERS: 200 TIME: 28
CUCKOO READS: 6228656
CHM WRITERS: 200 TIME: 32
CHM READS: 2473211
CUCKOO WRITERS: 100 TIME: 112
CUCKOO READS: 4040229
CHM WRITERS: 100 TIME: 20
CHM READS: 1887360
CUCKOO WRITERS: 50 TIME: 13
CUCKOO READS: 4218974
CHM WRITERS: 50 TIME: 13
CHM READS: 2073062
CUCKOO WRITERS: 10 TIME: 10
CUCKOO READS: 2572866
CHM WRITERS: 10 TIME: 14
CHM READS: 1481922
CUCKOO WRITERS: 1 TIME: 11
CUCKOO READS: 3072166
CHM WRITERS: 1 TIME: 15
CHM READS: 1126921
```

## Future Work

* We can match the write throughput via using granular locks for writers instead of single lock.
* Growth and Rehashing.
* Random Hash functions.