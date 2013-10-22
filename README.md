alter-datas
===========

Alternative Implementations of Basic Data Structures

HopscotchHash: A java based implementation of a single threaded hopscotch hash.  See the hopscotch hash paper (http://cs.nyu.edu/~lerner/spring11/proj_hopscotch.pdf) for the inspiration for this implementation.  

TODO: This implementation still needs full support for the Java Collections interface calls like entrySet().  It currently only supports the basic operations.

LinearIndexedHash: HopscotchHash without the proper swap implementation.  This will do a fixed lookup up to 32 entries in the future and do a rehash if it cannot find a place to store the entry close to the inital hash position.

