import os
import json

f_output1 = open("mapping.json",'w')
f_output2 = open("input_preprocessed.json",'w')

f_input = open("../data/input/Crimes_2013.json",'r')

d = json.load(f_input)
d_mapping = {}
d_preprocessed = {}

count = 1
for x in d["data"]:

	if (x[23] != None and x[24] != None):
		d_preprocessed[x[8]] = {}
		d_preprocessed[x[8]]["x_coordinate"] = x[23]
		d_preprocessed[x[8]]["y_coordinate"] = x[24]
		d_preprocessed[x[8]]["type"] = x[13]

		if x[13] not in d_mapping:
			d_mapping[x[13]] = count
			count += 1

for x in d_preprocessed:
	temp = d_preprocessed[x]["type"]
	d_preprocessed[x]["type"] = str(d_mapping[temp])

'''
Without Sampling
json.dump(d_mapping,f_output1)
f_output1.close()

json.dump(d_preprocessed,f_output2)
f_output2.close()

print "Total distinct features "+str(count)+'\n'
'''

'''
Sampling Code
'''

d_final = {}
d_mapping_final = {}

for x in d_preprocessed:
	temp = int(d_preprocessed[x]['type'])
	if temp not in d_final:
		print temp
		d_final[temp] = []
		d_final[temp].append(x)
	else:
		d_final[temp].append(x)

max_len = 600
counter = 0
d_temp_mapping = {}
d_temp_processed = {}

for x in d_mapping:
	print x
	if len(d_final[int(d_mapping[x])]) < max_len:
		
		d_mapping_final[x] = d_mapping[x]
		d_temp_mapping[int(d_mapping[x])] = {}
		counter += 1

for x in d_preprocessed:
	if int(d_preprocessed[x]['type']) in d_temp_mapping:
		
		d_temp_processed[x] = {}
		d_temp_processed[x] = d_preprocessed[x]

json.dump(d_mapping_final,f_output1)
f_output1.close()

json.dump(d_temp_processed,f_output2)
f_output2.close()

print "Counter = "+str(counter)


'''
Internal representation of dictionary and list structure
>>> for x in d["data"][0]:
...     print x
... 
6029125
E203856B-FECC-42C9-A802-660A3333911B
6029125
1399100557
878752
1399100557
878752
None
9566267
HX217110
2013-12-31T23:59:00
029XX N MILWAUKEE AVE
1153
DECEPTIVE PRACTICE
FINANCIAL IDENTITY THEFT OVER $ 300
FACTORY/MANUFACTURING BUILDING
False
False
1412
None
35
21
11
None
None
2013
2014-04-09T17:12:53
None
None
[None, None, None, None, None]
'''