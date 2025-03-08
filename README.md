# In-Memory Spatial-Keyword Indexing
## Quick Start

Run <code>NewMain.java</code> to run the program. All options, including the dataset selection, are implemented in menus.

## Requirements
IMPORTANT: Due to its size, the _Parks_ dataset is not included in the repository. You can download it from LINK. After downloading, extract the files and place them in the <code>resources/data</code> folder.

## Configuration
All parameters are set in the <code>NewMain.java</code> file. Simply change the values of the variables to change the configuration (ie. fanout).

## Datasets
The most important available datasets are:
- _Hotels_: A _very small_ dataset that represents a hotel chain in the USA.
- _Postal Codes_: A _small_ dataset of boundaries of Postal Codes areas.
- _Sports_: A _medium_ dataset of boundaries of Sports areas.
- _Parks_: A _heavy_ dataset of boundaries of Parks areas.

## Results
The query statistics are saved in the <code>resources/results/metrics</code> folder. The results are saved in csv and txt formats. The csv format is as follows:
```csv
qryType_param1,qryType_param2,qryType_param3, ...
result1_val1,result1_val2,result1_val3, ...
result2_val1,result2_val2,result2_val3, ...
...
```
The actual query results are saved in the <code>resources/results</code> folder.
Logs can be found in the <code>resources/logs</code> folder.

## License

This project is licensed under the LGPL 2.1 License - see the [LICENSE.md](LICENSE.md) file for details.

## Based on the following works
- https://libspatialindex.org
- https://github.com/rafi-kamal/Aggregate-Spatio-Textual-Query
- http://lisi.io/spatial-keyword%20code.zip
- https://tzaeschke.github.io/phtree-site/