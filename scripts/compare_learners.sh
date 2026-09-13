series=$1
experiment=$2

learners=("slstar" "sllambda" "sllambdaeq")
stat="Resets"
phases=("testing:" "Total:")

std_dev() {
    local B=("$@")
    if [ -z "$B" ]; then
        echo 0
    else
        awk -v var="${B[*]}" 'BEGIN {n=split(var,a," ");
            x=0;
            y=0;
            for (v in a) {x+=a[v]; y+=a[v]^2};
            print sqrt(y/length(a) - (x/length(a))^2);}'
    fi
}

print_val() {
    files="$3"
    for f in ${files[@]}; do
        cat $f | grep -q "Execution terminated abnormally"
        if [ $? -eq 1 ]; then
            val=$(cat $f | grep "$1" | sed "s/.*$2: //g" | cut -d ' ' -f 1 | sed "s/[,\}]//g") 
            printf "%5d, " $val
            return
        fi
    done
    printf ", "
}

print_stat() {
    files="$3"
    sum=0
    n=0
    vals=()
    for f in ${files[@]}; do
        cat $f | grep -q "Execution terminated abnormally"
        if [ $? -eq 1 ]; then
            val=$(cat $f | grep "$1" | sed "s/.*$2: //g" | cut -d ' ' -f 1 | sed "s/[,\}]//g") 
            sum=$((sum+val))
            vals[${#vals[@]}]=$val
            n=$((n+1))
        fi
    done
    if [ $n -gt 0 ]; then
        avg=$((sum/n))
        std=$(std_dev "${vals[@]}" | sed "s/,/\./")
        printf "%5d, %s, " $avg $std
    else
        printf ", , "
    fi
}

print_stat2() {
    files="$3"
    sum=0
    n=0
    vals=()
    for f in ${files[@]}; do
        cat $f | grep -q "Execution terminated abnormally"
        if [ $? -eq 1 ]; then
            printf "cat %s | grep %s\n" $f $1
            val=$(cat $f | grep "$1" | sed "s/.*$2: //g" | cut -d ' ' -f 1 | sed "s/[,\}]//g") 
            printf "val: %d\n" $val
            sum=$((sum+val))
            vals[${#vals[@]}]=$val
            n=$((n+1))
        fi
    done
    if [ $n -gt 0 ]; then
        avg=$((sum/n))
        std=$(std_dev "${vals[@]}" | sed "s/,/\./")
        printf "%5d, %s, \n" $avg $std
    else
        printf ", , "
    fi
}

print_count() {
    files="$1"
    n=0
    for f in ${files[@]}; do
        line=`cat $f | grep "Last EQ Test found a counterexample: false"`
        if [ "$line" == "Last EQ Test found a counterexample: false" ]; then
            n=$((n+1))
        fi
    done
    printf "%1d, " $n
#    printf "%1d/%1d, " $n ${#files[@]}
}

print_header() {
    printf "Series, "
    printf "system, "
    printf "sysLoc, "
    printf "sysTrans, "
    printf "sysReg, "
    printf "sysConsts, "
    columns=("Learner" "Sum")
    for column in ${columns[@]}; do
        for learner in ${learners[@]}; do
          printf "${learner}${stat}${column}Avg, "
          printf "${learner}${stat}${column}Std, "
      done
    done  
    for learner in ${learners[@]}; do
        printf "${learner}CEAvg, ${learner}CEStd, "
    done
    for learner in ${learners[@]}; do
        printf "${learner}LocAvg, ${learner}LocStd, "
        printf "${learner}TransAvg, ${learner}TransStd, "
        printf "${learner}RegAvg, ${learner}RegStd, "
    done            
    for learner in ${learners[@]}; do
        printf "${learner}wctLearnAvg, ${learner}wctLearnStd, "
        printf "${learner}wctTestAvg, ${learner}wctTestStd, "
    done            
    for learner in ${learners[@]}; do
        printf "${learner}Succ, "
    done
    printf "\n"
}

currdir="$PWD"
cd ..

if [ "$1" == "-h" ]; then
  print_header
  exit
fi

printf "%15s, %15s, " $series $experiment

for learner in ${learners[@]}; do
    if [ -d "results/$series/$experiment-$learner" ]; then
        files=($(ls results/$series/$experiment-$learner | grep 'log' | sed "s/\(.*\)/results\/$series\/$experiment-$learner\/\1/"))
        print_val "Sys. Locations" "Sys. Locations" $files
        print_val "Sys. Transitions" "Sys. Transitions" $files
        print_val "Sys. Registers" "Sys. Registers" $files
        print_val "Constants" "Constants" $files
        break
    fi
done

for phase in ${phases[@]}; do
    for learner in ${learners[@]}; do
        if [ -d "results/$series/$experiment-$learner" ]; then
            files=($(ls results/$series/$experiment-$learner | grep 'log' | sed "s/\(.*\)/results\/$series\/$experiment-$learner\/\1/"))
            print_stat "$phase" $stat $files
        else
            printf ",,"
        fi 
    done
done  
for learner in ${learners[@]}; do
    if [ -d "results/$series/$experiment-$learner" ]; then
        files=($(ls results/$series/$experiment-$learner | grep 'log' | sed "s/\(.*\)/results\/$series\/$experiment-$learner\/\1/"))
        print_stat "Counterexamples" "Counterexamples" $files
    else
        printf ",,"
    fi 
done
for learner in ${learners[@]}; do
    if [ -d "results/$series/$experiment-$learner" ]; then
        files=($(ls results/$series/$experiment-$learner | grep 'log' | sed "s/\(.*\)/results\/$series\/$experiment-$learner\/\1/"))
        print_stat "Hyp. Locations" "Hyp. Locations" $files
        print_stat "Hyp. Transitions" "Hyp. Transitions" $files
        print_stat "Hyp. Registers" "Hyp. Registers" $files
    else
        printf ",,,,,,"
    fi
done
for learner in ${learners[@]}; do
    if [ -d "results/$series/$experiment-$learner" ]; then
        files=($(ls results/$series/$experiment-$learner | grep 'log' | sed "s/\(.*\)/results\/$series\/$experiment-$learner\/\1/"))
        print_stat "learning \[ms\]" "learning \[ms\]" $files
        print_stat "ce searching \[ms\]" "ce searching \[ms\]" $files
    else
        printf ",,,,"
    fi
done

for learner in ${learners[@]}; do
    if [ -d "results/$series/$experiment-$learner" ]; then
        files=($(ls results/$series/$experiment-$learner | grep 'log' | sed "s/\(.*\)/results\/$series\/$experiment-$learner\/\1/"))
        print_count $files
    else
        printf "0,"
    fi
done
printf "\n"

cd "$currdir"

