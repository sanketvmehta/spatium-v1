try:
	import database.mysql as db
except ImportError as exc:
	print("Error: failed to import settings module ({})".format(exc))

class Compare():
	"""Class to compare results from proposed algorithm with results from baseline algorithm for correctness"""	

	def __init__(self, app_name = "spatium", dbname = "spatium", verbose = 0):	
		self.dbname = dbname
		self.app_name = app_name
		self.conn = db.connect(self.app_name, self.dbname)
		self.cursor = self.conn.cursor()
		self.verbose = verbose

	def compare_results_size2(self, label = 1, prosposed_results_file = "graph.txt"):
		"""Compare results for size 2 from proposed algorithm and baseline method for correctness label is the `label` corresponding to size 2 """
		D = {}
		sql_baseline = "SELECT instanceid1, instanceid2 FROM instance2"
		if label!=-1:
			sql_baseline+=" WHERE label = "+str(label)
		result = db.read(sql_baseline, self.cursor)
		for i in result:
			id1 = str(i[0])
			id2 = str(i[1])
			key = ""
			if(int(id1) > int(id2)):
				key = id1+":"+id2
			else:
				key = id2+":"+id1
			D[key] = "baseline"

		with open(prosposed_results_file) as graph:
			for line in graph:
				a = line.strip().split()
				id1 = str(a[0])
				id2 = str(a[1])
				if(int(id1) > int(id2)):
					key = id1+":"+id2
				else:
					key = id2+":"+id1
				if key not in D:
					D[key] = "proposed"
				else:
					D[key] = "match"

		match_count = 0
		proposed_count = 0
		baseline_count = 0
		for i in D:
			if D[i] == "match":
				match_count+=1
			elif D[i] == "proposed":
				proposed_count+=1
			else:
				baseline_count+=1

		print "baseline_count", baseline_count
		print "proposed_count", proposed_count
		print "match_count", match_count

	def compare_results_sizek(self, k = 2, label = -1, prosposed_results_file = "graph.txt"):
		"""Compare results for size k from proposed algorithm and baseline method for correctness label is the `label` corresponding to size 2
		All labels are matched if label = -1"""
		D = {}
		sql_baseline = "SELECT "
		for i in range (1,k+1):
			sql_baseline+="instanceid"+str(i)+", "
		sql_baseline = sql_baseline[:-2]
		sql_baseline+=" FROM instance"+str(k)
		if label!=-1:
			sql_baseline+=" WHERE label = "+str(label)
		result = db.read(sql_baseline, self.cursor)
		for i in result:
			key = ""
			for j in i:
				key+=str(j)+":"
			key = key[:-1]
			D[key] = "baseline"

		with open(prosposed_results_file) as graph:
			for line in graph:
				i = line.strip().split()
				key = ""
				for j in i:
					key+=str(j)+":"
				key = key[:-1]
				if key not in D:
					D[key] = "proposed"
				else:
					D[key] = "match"

		match_count = 0
		proposed_count = 0
		baseline_count = 0
		for i in D:
			if D[i] == "match":
				match_count+=1
			elif D[i] == "proposed":
				proposed_count+=1
			else:
				baseline_count+=1

		print "baseline_count", baseline_count
		print "proposed_count", proposed_count
		print "match_count", match_count

	def compare_results_size3(self, prosposed_results_file = "test/graph.txt"):
		"""Compare results for size 2 from proposed algorithm and baseline method for correctness label is the `label` corresponding to size 2 """
		D = {}
		sql_baseline = "SELECT typeid1, typeid2, typeid3 FROM candidate3"
		result = db.read(sql_baseline, self.cursor)
		for i in result:
			id1 = str(i[0])
			id2 = str(i[1])
			id3 = str(i[2])
			key = id1+":"+id2+":"+id3
			D[key] = "baseline"

		with open(prosposed_results_file) as graph:
			for line in graph:
				a = line.strip().split(":")
				print a
				id1 = str(a[0])
				id2 = str(a[1])
				id3 = str(a[2])
				key = id1+":"+id2+":"+id3
				if key not in D:
					D[key] = "proposed"
				else:
					D[key] = "match"

		match_count = 0
		proposed_count = 0
		baseline_count = 0
		for i in D:
			if D[i] == "match":
				match_count+=1
			elif D[i] == "proposed":
				proposed_count+=1
			else:
				baseline_count+=1

		print "baseline_count", baseline_count
		print "proposed_count", proposed_count
		print "match_count", match_count
