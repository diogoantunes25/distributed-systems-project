# DistLedger

Distributed Systems Project 2022/2023

## Authors

**A09**

### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __GXX__ with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for 
code dependency management, to ensure your code runs using the correct components and not someone else's.

### Team Members

| Number | Name              | User                                    | Email                                         |
|--------|-------------------|-----------------------------------------|-----------------------------------------------|
| 99210  | Diogo Antunes     | <https://github.com/diogoantunes25>     | <mailto:diogo.santiago@tecnico.ulisboa.pt>    |
| 99256  | Joao Rocha        | <https://github.com/Calhau18>           | <mailto:joao.t.f.rocha@tecnico.ulisboa.pt>    |
| 99220  | Francisco Lopes   | <https://github.com/franciscolopes22>   | <mailto:francisco.a.lopes@tecnico.ulisboa.pt> |

## Getting Started

The overall system is made up of several modules. The main server is the _DistLedgerServer_. The clients are the _User_ 
and the _Admin_. The definition of messages and services is in the _Contract_. The future naming server
is the _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/DistLedger) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules:

```s
mvn clean install
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
