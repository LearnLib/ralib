inputs=${1:-0}

#series=($(ls results))
series="benchmark"

models=()

for s in ${series[@]}; do
    experiments=($(ls results/$s | sed "s/\(.*\)-.*/\1/"))
    for e in ${experiments[@]}; do
        if [[ ! $(echo ${models[@]} | fgrep -w $e) ]]; then
            models[${#models[@]}]=$e
        fi
    done
done

for m in ${models[@]}; do
    echo ${m^^}
    ./collate_experiment.sh $m $inputs
    printf "\n\n"
done


