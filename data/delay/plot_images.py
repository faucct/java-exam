import matplotlib.pyplot as plt
from glob import glob
from itertools import groupby
import os
import sys

all_results = []
for path in glob(os.path.join(sys.path[0], '*/results.tsv')):
    with open(path) as file:
        header = next(file)[:-1].split("\t")
        for line in file:
            all_results.append(line[:-1].split("\t"))
# results = list(zip(*results))
for y in ['Server request duration', 'Processing request duration', 'Client request duration']:
    legend = []
    for architecture, results in groupby(all_results, lambda result: result[0]):
        results = list(results)
        legend.append(architecture)
        plt.plot(
            [int(dict(zip(header, result))['Delay']) / 1E6 for result in results],
            [float(dict(zip(header, result))[y]) / 1E6 for result in results],
        )
    plt.legend(legend)
    plt.xlabel('Delay (ms)')
    plt.ylabel(y + ' (ms)')
    plt.title('Clients number=30. Array size=1000')
    plt.draw()
    plt.savefig(os.path.join(sys.path[0], y + '.png'))
    plt.show()
