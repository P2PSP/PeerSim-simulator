import sys, getopt

def C(n, k):
	if 0 <= k <= n:
		ntok = 1
		ktok = 1
		for t in xrange(1, min(k, n - k) + 1):
			ntok *= n
			ktok *= t
			n -= 1
		return ntok // ktok
	else:
		return 0

def f(n, T):
	re = 0.0
	for i in range(1, n-T+1):
		re += i * (float(C(n-i-1, T-1)) / C(n-1, T))
	return re

def g(n, T, A):
	re = 0.0
	for i in range(1, A+1):
		re += f(n-i+1, T)
	return re

def main(argv):
	try:
		opts, args = getopt.getopt(argv, "t:a:n:", [])
	except getopt.GetoptError as err:
		print str(err)
		sys.exit(2)
	T = 1
	A = 1
	n = 9
	for o, a in opts:
		if o == "-t":
			T = int(a)
		elif o == "-a":
			A = int(a)
		elif o == "-n":
			n = int(a)
	print g(n, T, A)

if __name__ == "__main__":
	main(sys.argv[1:])		