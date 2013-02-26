Falkland-CMS
============

Falkland CMS is a Web Content/Collection Management System written in Clojure, ClojureScript and CouchDB.

## What is a CMS?

A CMS or [WCMS (web content management sysstem)](http://en.wikipedia.org/wiki/Web_content_management_system) is a system for publishing website content. Key foundational features of a WCMS are:

* separation of content and presentation, managing the data in the system separately from how it looks when presented
* easy editing of content, usually through an administrative web UI
* collaboration of multiple content authors
* templates to take the data in the system and present it as web content, typically HTML

Falkland CMS has these foundational capabilities.

## Why Falkland CMS?

Like its inspiration, [Omeka](http://omeka.org/about/), Falkland CMS sits at the intersection between a Web CMS, a Digital Collection Management system, and Museum Exhibit Management system.

It is ideal for:

* online museum exihibits
* repository and presentation of digital collections
* online presentation of library collections
* repository and presentation of primary and secondary source collections

Falklands CMS is for collecting and curating, not authoring. While Falkland CMS supports authoring of new content to provide context and exhibition of collected resources, it's not suitable as a WCMS for brand new content. You wouldn't run a daily newspaper with Falklankds CMS. It's primary role is to point to existing content, either content on the web, or content collected into Falklands CMS 

### Who is this for?

* scholars
* museum and exihibit curators
* librarians
* archivists
* historical organizations
* teachers and professors
* collectors / hobbyists / enthusiasts

### What are some (hypothetical) example uses of Falkland CMS?

* The Venerable VIC-20 - an online exihibit of everything Commodore VIC-20
* Camus.org - Primary and secondary source collection for the philosopher Albert Camus
* Mudskippers.org - a guide to all the world's knowledge about the amazing amphibian fish
* Jack Freeman's Library - presenting 40 years of one man's books
* nil.org - A complete guide to Nihilism 
* 8-bit '80's - a site to show off an extensive retro video game collection
* Pitiful Pirates - The most losingest team in the history of professional sports, Tampa Bay Buccaneers from 1976-1995

### Who is this not for?

* Anyone that needs granular [security](#security)
* Anyone that needs extensive new content authoring
* Anyone that needs extensive, professional-grade offline artifact cataloging

### Where'd the name came from?

Falkland CMS was built to support the [Falklandsophile](http://falklandsophile.com) website. So the name seemed fitting.

## Installation

### External Dependencies

Most of Falkland's dependencies are internal, meaning lein will handle getting them for you. There are a few exceptions:

* [Clojure](http://clojure.org/) - Clojure is a Lisp that runs on the Java VM
* [Java](http://www.java.com/) - a Java VM is needed to run Clojure
* [Leiningen](https://github.com/technomancy/leiningen) - Clojure's native build tool
* [CouchDB](http://http://couchdb.apache.org/) - CouchDB is a schema-free, document-oriented database, ideally suited for a CMS
* [elasticsearch](http://www.elasticsearch.org/) - elasticsearch is a schema-free, document-oriented search engine, ideally suited for CouchDB

## Quick Start Guide

## Concepts

### Items

### Taxonomies

### Faceted Search

### Pages

There are 3 types of pages

* **static** - A static page is a simple CMS page, it may reference named or searched for resources, but it could also be made up of just static content. A home page, about page, or terms of use page is a good example.
* **item** - An item page displays a single particular item of a particular item type
* **taxonomy** - A taxonomy page 

### <a name="security"/> Security Model

Users of Falkland CMS are one of three types:

* **Administrator** - named users that can administer the configuration of the system and all items
* **User** - named users that can create, update, and delete all items in the system, but not administer the configuration of the system
* **The General Public** - everyone accessing the system anonymously with their web browser

## Example Sites

* [Falklandsophile](http://falklandsophile.com)

## Contributing

## Getting Help

## License

Copyright © 2013 Snooty Monkey

Falkland CMS is distributed under the [MIT license](http://opensource.org/licenses/MIT).
