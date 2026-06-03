series=$1
experiment=$2
learner=$3

phases=("Testing" "Optimization" "Analysis" "Processing" "Total excl testing" "Total:")
headers=("Testing:          " "Optimization:     " "Analysis:         " "Processing:       " "Total excl tests: " "Total:            ")
stats=("Resets" "Inputs")

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
            printf "%s\t" $val
        else
            printf -- "-\t"
        fi
    done
    if [ $n -gt 0 ]; then
        avg=$((sum/n))
        std=$(std_dev "${vals[@]}" | sed "s/,/\./")
        printf "Avg: %d\tStd: %s\n" $avg $std
    else
        printf "Avg: -\tStd: -\n"
    fi
}

print_dashes() {
    for i in $(seq $1); do
        printf '-'
    done
    printf '\n'
}



count=$((${#1}+${#experiment}+${#learner}+10))
print_dashes $count
echo "--- $1/$experiment-$learner ---"
print_dashes $count

currdir="$PWD"
cd ..

if [ -d "results/$1/$2-$3" ]; then
    files=($(ls results/$1/$2-$3 | grep 'log' | sed "s/\(.*\)/results\/$1\/$2-$3\/\1/"))
    printf "Locations:        "
    print_stat "Hyp. Locations" "Hyp. Locations" $files
    printf "Transitions:      "
    print_stat "Hyp. Transitions" "Hyp. Transitions" $files
    printf "Registers:        "
    print_stat "Hyp. Registers" "Hyp. Registers" $files
    printf "Constants:        "
    print_stat "Constants" "Constants" $files
    printf "Counterexamples:  "
    print_stat "Counterexamples" "Counterexamples" $files
    printf "CE max length:    "
    print_stat "CE max length" "CE max length" $files
    printf "CE avg length:    "
    print_stat "CE avg length" "CE avg length" $files
    for stat in ${stats[@]}; do
        echo $stat | tr '[:lower:]' '[:upper:]'
        for index in ${!phases[@]}; do
            printf "%s" "${headers[$index]}"
            print_stat "${phases[$index]}" $stat $files
        done
    done
    printf "Successful: %d/%d\n" $n ${#files[@]}
fi

cd "$currdir"

