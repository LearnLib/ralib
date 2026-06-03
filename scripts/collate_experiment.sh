experiment=$1
inputs=$2

all_series=$(ls results)

headers=("Counterexamples" "CE max length" "CE avg length" "Optimization" "Analysis" "Processing" "Total excl tests")
learners=("slstar" "sllambda" "slleq")

print_averages() {
    for h in "${headers[@]}"; do
        avg=($(echo "$1" | grep "$h" | sed "s/.*Avg: \([0-9]\+\).*/\1/"))
        if [ $inputs -eq 0 ]; then
            printf ";%s" $avg
        else
            if [ ${#avg[@]} -gt 0 ]; then
                printf ";%s" ${avg[$((${#avg[@]}-1))]}
            fi
        fi
    done
}

print_stddevs() {
    for h in "${headers[@]}"; do
        std=($(echo "$1" | grep "$h" | sed "s/.*Std: \([\.0-9]\+\)/\1/"))
        #printf ";%s" $std
        if [ $inputs -eq 0 ]; then
            printf ";%s" $std
        else
            if [ ${#std[@]} -gt 0 ]; then
                printf ";%s" ${std[$((${#std[@]}-1))]}
            fi
        fi
    done
}

print_separators() {
    for i in $(seq $1); do
        printf ';'
    done
}

print_successful() {
    successful=$(echo "$1" | grep "Successful" | sed "s/.*\([0-9]\+\)\/[0-9]\+/\1/")
    total=$(echo "$1" | grep "Successful" | sed "s/.*[0-9]\+\/\([0-9]\+\)/\1/")
    if [ -n "$total" ]; then
        printf "Successful: %s of %s" $successful $total
    fi
}

for i in ${learners[@]}; do
    for h in "${headers[@]}"; do
        printf ";%s" "$h"
    done
    printf ";"
done
printf "\n"

for series in ${all_series[@]}; do
    not_empty=0
    tables=()
    for l in ${learners[@]}; do
        tables[${#tables[@]}]=$(./search_logs.sh $series $experiment $l)
        if [ -d "../results/$series/$experiment-$l" ]; then
            not_empty=1
        fi
    done
    if [ $not_empty -eq 1 ]; then
        printf "%s" $series
        for index in ${!learners[@]}; do
            start=$((${#headers[@]}/2))
            end=$((${#headers[@]}-$start-1))
            print_separators $start
            printf "%s;;" ${learners[$index]^^}
            print_successful "${tables[$index]}"
            print_separators $end
        done

        printf "\nAverage"
        for table in "${tables[@]}"; do
            print_averages "$table"
            printf ';'
        done
        printf "\nStd dev"
        for table in "${tables[@]}"; do
            print_stddevs "$table"
            printf ';'
        done

        printf "\n\n"
    fi
done
