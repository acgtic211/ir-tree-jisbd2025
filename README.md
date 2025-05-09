# In-Memory Spatial-Keyword Indexing
## Quick Start

Run <code>NewMain.java</code> to run the program. All options, including the dataset selection, are implemented in menus.

## Requirements
IMPORTANT: Due to its size, the _Parks_ dataset is not included in the repository. You can download it from [LINK](https://drive.google.com/file/d/1me1-s-4F8_odf378kIPaTg8uEZ-52RrU/view?usp=sharing). After downloading, extract the files and place them in the <code>resources/data</code> folder.

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

## Using Docker

To run this program using Docker Compose:

1. Make sure you have Docker and Docker Compose installed on your system.
2. Navigate to the root directory of the project.
3. Run the following command:

```bash
docker-compose up --build
```

This will build the Docker image, compile the application with Maven, **automatically download the Parks dataset** if it doesn't exist locally, and run the Java program. Results will be saved in the `resources/results` directory.

To run with specific JVM options or arguments:

```bash
docker-compose run --rm -e JAVA_OPTS="-Xmx4g" spatio-textual-index java -jar target/InMemory-Spatio-Textual-Index-1.0-SNAPSHOT.jar
```

## License

This project is licensed under the LGPL 2.1 License - see the [LICENSE.md](LICENSE.md) file for details.

## Based on the following works
- https://libspatialindex.org
- https://github.com/rafi-kamal/Aggregate-Spatio-Textual-Query
- http://lisi.io/spatial-keyword%20code.zip
- https://tzaeschke.github.io/phtree-site/*