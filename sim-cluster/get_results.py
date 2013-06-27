'''
Created on 22/04/2013

@author: jalvaro
Read timing info from files. For measuring the performance of simulations.
Execution: python get_results.py ruta tam_buf
'''
import sys
import glob

sum = float(0)
count = 0
fail_count = 0
avg = float(0)
sum_errors_buf = int(0)
count_errors_buf = int(0)
avg = float(0)
#files_path = glob.glob('/home/jalvaro/workspaces-eclipse/P2PSP-sim-cluster/sim/sim-cluster/timing/*')
tam_buf = 0

try:
    if(sys.argv[1].endswith('/')):
        files_path = glob.glob(sys.argv[1]+'*')
    else:
        files_path = glob.glob(sys.argv[1]+'/*')
    tam_buf = int(sys.argv[2])
except:
    print('Incorrect arguments. Usage: python get_timing.py ruta tam_buf')
    sys.exit()

#print(files_path)
for file_path in files_path:
    fr = open(file_path,'r')
    try:
        #read avg time
        line = fr.readline()
        time = float(line.split()[1])
        print('Time '+str(time))
        sum += time
        count += 1
        #read num_errors_buf
        fr.readline()
        line = fr.readline()
        num_errors_buf = float(line.split()[1])
        print('Num errors buf '+str(num_errors_buf))
        sum_errors_buf += num_errors_buf
        count_errors_buf += 1
    except:
        fail_count += 1

avg = sum / count
avg_errors_buf = sum_errors_buf / count_errors_buf
avg_perc_errors_buf = avg_errors_buf *100 / (tam_buf/2)
print('')
print('Avg time: ' + str(avg) + ' secs')
print('Avg num errors buf ' + str(avg_errors_buf))
print('Avg % errors buf '+str(avg_perc_errors_buf))
print('')
print(str(count) + ' valid values read')
print(str(fail_count) + ' invalid values read')    
    
        



