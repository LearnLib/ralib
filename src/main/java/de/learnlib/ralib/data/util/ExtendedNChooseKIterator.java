/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.learnlib.ralib.data.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author fh
 */
public class ExtendedNChooseKIterator<N extends Comparable<N>,K extends Comparable<K>>
{

    // TODO: port this iterator if needed or remove it
    
//    private List<N> n;
//    private List<K> k;
//
//    private int[] current;
//
//    private int elements = 1;
//
//    private ArrayList<Integer> positons = new ArrayList<Integer>();
//
//    private boolean invalid = false;
//    
//    private boolean removeDoubles = true;
//
//    public ExtendedNChooseKIterator(Collection<N> n, Collection<K> k)
//    {
//        if (n.size() < 1 || k.size() < 1)
//        {
//            invalid = true;
//            return;
//        }
//
//        this.n = new ArrayList<N>(n);
//        this.k = new ArrayList<K>(k);
//
//        current = new int[k.size()];
//        for (int i=0;i< current.length;i++)
//            current[i] = 0;
//
//        positons.add(0);
//        current[0] = 0;
//    }
//
//    public ExtendedNChooseKIterator(Collection<N> n, Collection<K> k, boolean surjective, boolean removeDoubles)
//    {
//        this(n,k,surjective);
//        this.removeDoubles = removeDoubles;
//        
//        if(!removeDoubles && surjective && !invalid) 
//        {
//            positons.clear();
//            for (int i=0;i < k.size();i++)
//            {
//                positons.add(i);
//                current[i] = 1;
//            }           
//            current[current.length-1]--;
//        }
//    }
//
//            
//    public ExtendedNChooseKIterator(Collection<N> n, Collection<K> k, boolean surjective)
//    {
//        this(n,k);
//        if (invalid)
//            return;
//
//        if (!surjective)
//            return;
//
//        if (n.size() < k.size())
//            invalid = true;
//
//        for (int i=0;i < k.size();i++)
//        {
//            positons.add(i);
//            current[i] = i+1;
//        }
//
//        current[current.length-1]--;
//
//        elements = k.size();
//
//    }
//
//
//    public Mapping<N,K> nextMapping()
//    {
//        if (!next())
//            return null;
//
//        Mapping<N,K> m = new Mapping<N,K>();
//
//        for (int key : positons)
//            m.add(n.get(current[key]-1), k.get(key));
//
//        return m;
//    }
//
//    public Mapping<K,N> nextInvereseMapping()
//    {
//        if (!next())
//            return null;
//
//        Mapping<K,N> m = new Mapping<K,N>();
//
//        for (int key : positons)
//            m.add(k.get(key),n.get(current[key]-1));
//
//        return m;
//    }
//
//    public boolean next()
//    {
//        if (invalid)
//            return false;
//
//        int toadd = 0;
//
//        while (true)
//        {
//
//            int cpos = positons.get(positons.size()-1);
//
//            current[cpos]++;
//            if (removeDoubles)
//                removeDoubles(cpos);
//
//            // value overflow
//            if (current[cpos] > n.size())
//            {
//                current[cpos] = 0;
//                cpos++;
//                positons.remove(positons.size()-1);
//                positons.add(cpos);
//
//                // position overflow
//                if (cpos + toadd >= k.size())
//                {
//                    toadd++;
//                    positons.remove(positons.size()-1);
//
//                    if (positons.size() > 0)
//                        continue;
//                }
//                else
//                {
//                    current[cpos] = 1;
//                    if (removeDoubles)
//                        removeDoubles(cpos);
//                }
//            }
//            else if (toadd < 1)
//            {
//                return true;
//            }
//
//            // add missing
//            // too many -> start over?
//            if (cpos + toadd >= k.size())
//            {
//                elements++;
//                if (elements > java.lang.Math.min(n.size(), k.size()))
//                        return false;
//
//                positons.clear();
//                for (int i=0;i< current.length;i++)
//                    current[i] = 0;
//
//                for (int i=0;i< elements; i++)
//                {
//                    current[i] = i+1;
//                    positons.add(i);
//                }
//
//                return true;
//            }
//
//            for (int i=cpos+1;i <= cpos + toadd;i++)
//            {
//                positons.add(i);
//                current[i] = 1;
//                if (removeDoubles)
//                    removeDoubles(i);
//            }
//
//            return true;
//
//        }
//    }
//
//    
//    private void removeDoubles(int cpos)
//    {
//            // check doubles
//            boolean doubles = false;
//            do
//            {
//                doubles = false;
//                for (int p : positons)
//                    if (p != cpos)
//                        if (current[cpos] == current[p])
//                        {
//                            current[cpos]++;
//                            doubles = true;
//                            break;
//                        }
//            }
//            while (doubles);
//    }
//
//    @Override
//    public String toString()
//    {
//        String a = "[";
//        for (int i : current)
//            a += i + ",";
//
//        return a.substring(0,a.length()-1) + "]";
//    }

}
