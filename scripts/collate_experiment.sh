series=$1
experiment=$2

stats=("Locations" "Transitions" "Registers" "Constants" "Analysis" "Processing" "Total excl tests")
headers=("Locations" "Transitions" "Registers" "Constants" "CE Analysis Resets" "Hyp Construction Resets" "Total Resets")
learners=("slstar" "sllambda" "slleq")

print_averages() {
    for h in "${stats[@]}"; do
        avg=($(echo "$1" | grep "$h" | sed "s/.*Avg: \([0-9]\+\).*/\1/"))
        printf ";%s" $avg
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

printf "${experiment^^}"

for h in "${headers[@]}"; do
    printf ";%s" "$h"
done
printf "\n"

for learner in ${learners[@]}; do
    not_empty=0
    tables=()
    table=$(./search_logs.sh $series $experiment $learner)
    if [ -d "../results/$series/$experiment-$learner" ]; then
        not_empty=1
    fi
    if [ $not_empty -eq 1 ]; then
        printf "${learner}"
        print_averages "$table"
        printf "\n"
    fi
done
