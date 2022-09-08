import matplotlib.pyplot as plt

latencies = [4.3, 8.1, 7, 6.1, 5.1, 4.3, 3.7]
bandwidth_overhead = [227, 74, 101, 77, 101, 73, 98]

protocols = ['Legacy flooding', 'Erlay-a-0.01', 'Erlay-b-0.01', 'Erlay-c-0.01', 'Erlay-d-0.01',
             'Erlay-e-0.01', 'Erlay-f-0.01']


fig, ax = plt.subplots()
ax.set_xlabel('Latency (s)')
ax.set_ylabel('Bandwidth overhead (bytes per tx)')
ax.scatter(latencies, bandwidth_overhead)

for i, txt in enumerate(protocols):
    if i == 0:
        ax.annotate(txt, (latencies[i], bandwidth_overhead[i] - 10))
    elif i == 1:
        ax.annotate(txt, (latencies[i] - 0.3, bandwidth_overhead[i] + 5))
    else:
        ax.annotate(txt, (latencies[i], bandwidth_overhead[i] + 5))

plt.show()
