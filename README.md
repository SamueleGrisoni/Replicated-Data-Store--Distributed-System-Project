# Replicated, highly-available data store

This repo implements a highly-available, replicated key value store, that offers two primitives:

- read(key)
- write(key, value)

![context C4 diagram](./design/finalDesign/C4/context.png)


The store is implemented by N servers. Each of them contains a copy of the entire store and cooperate to keep a consistent view of this replicated store. In particular, the system provide a causal consistency model.

The system is highly available, meaning that a client can continue to fully operate (read and write) as soon as it remains connected to its server, even if that server is disconnected from other servers in the system.

The assumption under which the system is able to operate is that:

- disk persistance survives server failure
- omission failure only (no byzantine)
- servers and channel can fail

The system is implemented in Java. To see an examples on how it works it is sufficient to do as follows: 

<code> java -jar Server.jar configFilePath </code> examples of configFiles can be found in [here](./configFiles/)

<code> java -jar Client.jar </code>

Additinal documentation on the design of the project can be found in the [desing](./design/finalDesign/) folder
