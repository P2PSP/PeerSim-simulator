import getopt, sys, re

def main(argv):
	try:
		opts, args = getopt.getopt(argv, "i:", [])
	except getopt.GetoptError as err:
		print str(err)
		sys.exit(2)
	if len(opts) == 0:
		sys.exit(2)

	logFile = opts[0][1]
	poisonedChunksSum = 0
	experiments = 0
	p = re.compile("==\s(\d+)")
	with open(logFile) as f:
		for line in f:
			m = p.search(line)
			if m:
				experiments += 1
				poisonedChunksSum += int(m.group(1))

	print float(poisonedChunksSum) / experiments

if __name__ == "__main__":
	main(sys.argv[1:])