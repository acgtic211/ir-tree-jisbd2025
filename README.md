# In-Memory Spatial-Keyword Indexing
## Quick Start

Run <code>AppMain.java</code> to run the program. All options, except the dataset selection, are implemented in menus.

To change datasets:
1. Edit <code>AppMain.java</code> and uncommet the dataset to use (both keywords and locations)
2. Edit <code>spatialindex/parameters/Parameters.java</code> to uncomment the correct paramenter for the selected dataset.

To save the query results to disk, set <code>writeDebugQueryResults = true</code> in <code>AppMain.java</code>. Results and statistics are save inside <code>resources</code>.

## License

This project is licensed under the LGPL 2.1 License - see the [LICENSE.md](LICENSE.md) file for details.

## Based on the following works
- https://libspatialindex.org
- https://github.com/rafi-kamal/Aggregate-Spatio-Textual-Query
- http://lisi.io/spatial-keyword%20code.zip