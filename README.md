# APT-Search-Engine

This repository will contain all APT project files and any updates will be committed here.

## Milestone 1 (Crawler):
- [ ] Implement a web crawler that collects documents from the web and downloads them.
  The crawler has the following properties:
    - [x] The crawler must not visit the same URL more than once.
    - [x] The crawler can only crawl documents of specific types (HTML is sufficient for the project).
    - [ ] The crawler must maintain its state so that it can, if interrupted, be started again to crawl the documents
          on the list without revisiting documents that have been previously downloaded.
    - [x] Some web administrators choose to exclude some pages from the search such as their web pages.
    - [ ] Frequency of crawling is an important part of a web crawler. Some sites will be visited more often than
          others. You may set some criteria to the sites.
    - [ ] Provide a multithreaded crawler implementation where the user can control the number of threads
          before starting the crawler.
    - [x] Implement a web crawler that conforms to robot exclusion standard.
    - [x] Take Care of the choice of your seeds and the stopping criteria.
    
## Milestone 2 (Indexer):
- [ ] Implement an indexer To respond to user queries fast enough using the data obtained from the web crawler.
  - [ ] The index has to be maintained in secondary storage. You can implement your own file
structure or use a database.
  - [ ] Fast Retrieval: The index must be optimized for responding to queries like:
  - [ ] The set of documents containing a specific word (or set of words)
  - [ ] The set of words contained in a specific document.
  - [ ] Incremental Update: It must be possible to update an existing index with a set of newly crawled HTML
documents.
  
