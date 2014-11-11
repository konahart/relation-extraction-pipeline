# README #

This is a pipeline for creating training (and testing) data for the relation
extraction system known as MultiR. Please note that the included 
implementations should be considered "baseline." Further development is 
encouraged. 
Although the resources included at this time only support English, the design
of the pipeline allows other linguistic resources to be used. 

### Components of a Pipeline ###

* Corpus Conversion - Interface: CorpusConverter - Converts the corpus into the
 datastructure used by the pipeline, e.g, Gigaword to AnnotatedDoc
* Document Processing - Interface: DocProcessor - Performs whatever is needed 
to recognize entities in the text. Is used for preprocessing and feature 
generation for MultiR.  
* Entity Linking - Interface: Linker - Takes the mentions found in the Document
Processing step and links them to candidate entities in the entity database 
(Freebase). 
* Relation Annotation - Interface: RelationAnnotator - Finds relations between
candidate entities and uses them to produce sentential annotations. 
* MultiR Compiling - (no Interface, just MultiRCompiler class) - Converts
datastructure used by the pipeline into format that can be read by MultiR. 

![GenericTrainingPipeline.png](https://cloud.githubusercontent.com/assets/945693/4819281/3b3afaf4-5f00-11e4-81bd-507311bebb94.png)

### Included Implementations ###

* CorpusConverter
    - SAXGigawordConverter
    - StripSGML
* DocProcessor
    - CoreNLPProcessor
* Linker
    - MapBasedEntityLinker
* RelationAnnotator
    - LuceneRelationSearcher
* No Interface
    - MultiRCompiler
    - Utils: provides (static) methods for taking care of file I/O and JSON 
de/serialization for the other classes.
* Data Structures
    - MultiRInstance: Consists of a bundle of Strings, one for each file that 
MultiRCompiler will output. It is also where the output is format, i.e., if 
the output format of MultiRCompiler needs to be changed, it should be done by 
editing MultiRInstance.
    - AnnotatedDoc: More details below (Pipeline datastructure)
    - Sentence: More details below (Pipeline datastructure)
    - Mention: More details below (Pipeline datastructure)
    - RelationAnnotation: More details below (Pipeline datastructure)

Note that each of the provided implementations (besides the CorpusConverters, 
which are called as part of DocProcessor) can be run on its own using the main
method. Because each step outputs its AnnotatedDocs, this allows each step to
be run outside of the pipeline, e.g., once preprocessing is completed, it does
not need to be rerun in order to try out different methods of entity linking. 

![PipelineDemo.png](https://cloud.githubusercontent.com/assets/945693/4819283/3b6a24e6-5f00-11e4-9732-45a83d49f0b2.png)

### Getting set up ###

**Maven setup**:

1. Install [Maven](http://maven.apache.org/download.cgi#Installation).

2. Navigate to the repo directory, where pom.xml is located.

3. Execute `mvn package`. The first time you run it will take a while while it
downloads all the dependencies declared in pom.xml. After that it will run much
more quickly.

### Additional resources set up ###

**Scripts**
Various scripts are provided for convenience of not having to retype the same
arguments over and over. They are located in the scripts directory, and more
detailed information about them is available in scripts/README-scripts. Please
note that since these scripts are provided for convenience only, some setup
will need to be done to set said arguments correctly.

**Entity alias file**
Certain implementations of the Entity Linking component, such as
MapBasedEntityLinker, require a tab-separated enitity-alias file. 
An example can be found under data/examples/exampleAliases.txt. 

**Relations file**
Depending on the implementation of the Relation Annotator component, such as 
MapBasedEntityLinker, a tab-separated enitity-alias file may be required. 
An examples can be found under data/examples/exampleRelations.txt.

**Relations index**
Additionally, if using Lucene-based components, such as LuceneRelationSearcher,
you will need to generate an index for it to use. There should be an associated
indexer class, such as LuceneRelationIndexer. Simply call this with a relations
file in the above format and an output directory in which to put the index.
This can be done using scripts/indexRelations.pbs.

### PipelineDemo details ###
The PipelineDemo class is included to give an example of how to code a 
pipeline. It is built to process English Gigaword text. It uses the following
classes:

* Document Processing - CoreNLPProcessor. Uses Stanford CoreNLP tools 
(English). 
* Corpus Conversion - SAXGigawordConverter. This is actually called inside
CoreNLPProcessor, instead of as a separate step. 
* Entity Linking - MapBasedEntityLinker. As described, uses a hashmap to
connect aliases to entities.
* Relation Annotation - LuceneRelationSearcher
* MultiR Compiling - MultiRCompiler

Note that the DocProcessor (CoreNLPProcessor) is passed to MultiRCompiler.
This is because MultiRCompiler only takes care of outputting the files MultiR
expects --- it should be totally isolated from any NLP steps so that different
DocProcessors can be substituted in. This way, the only step of the pipeline
that has to "worry" about the language being processed is the DocProcessor. As
long as UTF-8 encoding works, CorpusConverter, Linker, RelationAnnotator and
MultiRCompiler should not have enough interaction with the language to cause
any problems. 

How it works:

1.  preprocessing: CoreNLPProcessor calls SAXGigawordConverter to convert
Gigaword files into AnnotatedDocs. It then performs preprocessing on those
documents to get only "valid" sentences, in this case: longer than 6 tokens
(words), shorter than 50 tokens, and contains at least 2 entities. All other
sentences are discarded. Documents that contain no valid sentences are
discarded. The newly processed documents are outputted to a subdirectory of the
output directory called docs. Optionally, XML for each valid sentence is
outputted to a subdirectory of docs called xml (see note below). The sentences
are only partially processed at this point to avoid performing NLP on sentences
that will be filtered out further along the pipeline. 

2. linking: MapBasedEntityLinker loads a file of entity-alias pairs into a
HashMap that associates each alias with all the entities is may represent
(candidates), e.g., "John" may have candidates "John Smith", "John Doe", and
"John Appleseed". When MapBasedEntityLinker is given an AnnotatedDoc to link,
for each mentition string in the AnnotatedDoc it checks if that string is a key
in the HashMap. If it is, is adds all the candidates in the map to the mention
for that sentence. If a sentence has fewer than two entities that have mentions
found in the map, the sentence is removed from the AnnotatedDoc.

3. annotating: LuceneRelationSearcher requires that LuceneRelationIndexer has
already been run, and that a relation index exists. Given that, it takes every
pair of entities present in a sentence, and searches for every pair of
candidates for these two entities, in alternating order (remembering that
relations are fixed-order). If a positive relation is found for at least one
pair of candidates for a pair of entities, then the positive relation is added
to the sentence. If a pair of entities has no candidate pairs with relations,
then all candidate pairs are added to the sentence as empty relations. 

4. output: MultiRCompiler is called, which in turn calls CoreNLP to process the
linked and annotated AnnotatedDocs, and convert them into MultiRInstances.
CoreNLP performs tokenization, POS and NER tagging, and dependency parsing.
MultiRInstance takes empty relations and interprets them as negative examples.
MultiRCompiler should be completely isolated from the processing and
AnnotatedDocs -- its only job is to output the Strings stored in
MultiRInstances.

Note: CoreNLP has the option to output its annotations as XML. This option can
be turned on by uncommenting the line
```
docProc.setXMLOutput(true);
```
in PipelineDemo's constructor. This will be useful if there is ever a way to
read deserialize the annotations back into CoreNLP's Annotation datastructure,
to eliminate reanotation of sentences that currently occurs in the processing
step in order to add the dependency parse. 

### Output directory structure ###
Although the structure depends on how the pipeline is written, here is the
default/recommended directory structure:

```
Outputdir/
|--SENTDEPENDENCYINFORMATION
|--sentences.meta
|--SENTOFFSETINFORMATION
|--SENTTEXTINFORMATION
|--TOKENNERINFORMATION
|--TOKENOFFSETINFORMATION
|--TOKENPOSINFORMATION
|--training.inst
|--docs
|  |--inputfile.processed.gz
|  |--inputfile.linked.gz
|  |--inputfile.relations.gz
|  |--xml (optional)
|  |  |--inputfile.00.xml.gz
|  |  |--inputfile.01.xml.gz
|  |  |--inputfile.02.xml.gz
|--output
|  |--inputfile.error
|  |--inputfile.output
```

It is recommended that the MultiR files in the top directory are compiled from
all the files under docs/

### Pipeline datastructure ###

The main datastructure used by this pipeline is AnnotatedDoc. Each AnnotatedDoc
represents one article from Gigaword. It contains:

* a document ID

* a set of sentences

The sentences are a datastructure called Sentence. Each one contains:

* the sentence string

* the start offset relative to the original document

* the end offset relative to the original document

* a map of mention strings to mentions

* a set of relation annotations

The Mention datastructure contains:

* the start offset of the mention relative to the encapsulating sentence

* the end offset relative to the encapsulating sentence

* the NER type of the mention (e.g., PERSON, ORGANIZATION, LOCATION, etc)

* a set of candidate entities that the mention may be referring to


The RelationAnnotation datastructure contains:

* the string of the first entity of a relation

* the start offset of the first entity of a relation

* the end offset of the first entity of a relation

* the string of the second entity of a relation

* the start offset of the second entity of a relation

* the end offset of the second entity of a relation

* a set of relation between the first and second entities

### Example ###

Given an original document like: 

```
<DOC id="Test1" type="story" >

<HEADLINE>

Fake headline

</HEADLINE>

<DATELINE>

CAIRO, Jan. 1 (Xinhua)

</DATELINE>

<TEXT>

Mentioning Sa, Mel, Melissa Hart and Laura, Paula, Paula J Hart should
definitely bring up at least a people.person.parents relation. If not also a
child relation.

</TEXT>

</DOC>
```


At each step in the pipeline, the list of AnnotatedDocs (and everything they
contain) is output to a file in JSON format. When all the steps have been
completed, it will look like this:

```
[ {

  "sentences" : [ {

    "sentence" : "Mentioning Sa, Mel, Melissa Hart and Laura, Paula, Paula J
Hart should definitely bring up at least a people.person.parents relation.",

    "start" : 1,

    "relations" : [ {

      "relations" : [ "people.person.children" ],

      "entity1" : "m.06538bj",

      "e1StartSpan" : 38,

      "e1EndSpan" : 43,

      "entity2" : "m.012_53",

      "e2StartSpan" : 21,

      "e2EndSpan" : 33

    }, {

      "relations" : [ "people.person.children" ],

      "entity1" : "m.06538bj",

      "e1StartSpan" : 38,

      "e1EndSpan" : 43,

      "entity2" : "m.012_53",

      "e2StartSpan" : 16,

      "e2EndSpan" : 19

    }, {

      "relations" : [ "people.person.children" ],

      "entity1" : "m.06538bj",

      "e1StartSpan" : 52,

      "e1EndSpan" : 64,

      "entity2" : "m.012_53",

      "e2StartSpan" : 21,

      "e2EndSpan" : 33

    }, {

      "relations" : [ "people.person.children" ],

      "entity1" : "m.06538bj",

      "e1StartSpan" : 52,

      "e1EndSpan" : 64,

      "entity2" : "m.012_53",

      "e2StartSpan" : 16,

      "e2EndSpan" : 19

    }, {

      "relations" : [ "people.person.parents" ],

      "entity1" : "m.012_53",

      "e1StartSpan" : 21,

      "e1EndSpan" : 33,

      "entity2" : "m.06538bj",

      "e2StartSpan" : 38,

      "e2EndSpan" : 43

    }, {

      "relations" : [ "people.person.parents" ],

      "entity1" : "m.012_53",

      "e1StartSpan" : 21,

      "e1EndSpan" : 33,

      "entity2" : "m.06538bj",

      "e2StartSpan" : 52,

      "e2EndSpan" : 64

    }, {

      "relations" : [ "people.person.parents" ],

      "entity1" : "m.012_53",

      "e1StartSpan" : 16,

      "e1EndSpan" : 19,

      "entity2" : "m.06538bj",

      "e2StartSpan" : 38,

      "e2EndSpan" : 43

    }, {

      "relations" : [ "people.person.parents" ],

      "entity1" : "m.012_53",

      "e1StartSpan" : 16,

      "e1EndSpan" : 19,

      "entity2" : "m.06538bj",

      "e2StartSpan" : 52,

      "e2EndSpan" : 64

    } ],

    "mentionInstances" : [ "Laura", "Paula J Hart", "Melissa Hart", "Paula", 
"Mel" ],

    "mentions" : {

      "Laura" : {

        "spanStart" : 38,

        "spanEnd" : 43,

        "type" : "PERSON",

        "candidates" : [ "m.0k3q4ky", "m.0n3vphn", "m.04v6d84", "m.09sg9b",
"m.059xkp_", "m.04ls66", "m.02x0ngx", "m.07chj_c", "m.0j14xxd", "m.06538bj",
"m.09gly4g", "m.0w2ws9g", "m.0bdty6b", "m.0pd6kxc", "m.0bqskbr", "m.0x19t_d",
"m.02vsw78", "m.0y7ntdm", "m.0bv4ng8", "m.0qs3194", "m.07k7c1f", "m.04jfj3v",
"m.0zbg0zs", "m.01pngcc", "m.0j1374q", "m.0j36k22" ]

      },

      "Paula J Hart" : {

        "spanStart" : 52,

        "spanEnd" : 64,

        "type" : "PERSON",

        "candidates" : [ "m.06538bj" ]

      },

      "Melissa Hart" : {

        "spanStart" : 21,

        "spanEnd" : 33,

        "type" : "PERSON",

        "candidates" : [ "m.012_53" ]

      },

      "Paula" : {

        "spanStart" : 45,

        "spanEnd" : 50,

        "type" : "PERSON",

        "candidates" : [ "m.0fgxtx", "m.0b3y2zz", "m.02qs76f", "m.0hpqyds",
"m.0y7qv0r", "m.0ncqy5k", "m.0t4xt55" ]

      },

      "Mel" : {

        "spanStart" : 16,

        "spanEnd" : 19,

        "type" : "PERSON",

        "candidates" : [ "m.06tnl7", "m.012_53", "m.0c5vh", "m.0krnp29",
"m.0643kz6", "m.0gbyqcn", "m.0h3vnm4", "m.0_74v30", "m.0fzbhh", "m.0264kdz",
"m.0314hm", "m.02kfxx2", "m.05mzwzv", "m.02hrh2h", "m.04mn7c7", "m.06429cm",
"m.064xh5", "m.01_xqj", "m.02ppwfh", "m.0gdljzc", "m.0k26zt_", "m.01vlqt2",
"m.053hkl", "m.0zwyljx", "m.0h8gm3t", "m.0_qqsjx", "m.0h66xv", "m.0v8_7z1",
"m.0v04c_4", "m.0k8kqjh", "m.0vsmgvb", "m.0hfj159", "m.07wlpq", "m.0555z",
"m.03jnxcx", "m.0j3560d", "m.0gbzwzh", "m.09gj65", "m.0r9qt3m" ]

      }

    },

    "end" : 134

  } ],

  "id" : "Test1"

} ]
```

Notice that because the second sentence did not contain any entities, and 
therefore could not contain any relations, it was dropped.
